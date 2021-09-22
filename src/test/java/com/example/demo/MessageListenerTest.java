package com.example.demo;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageListenerTest {
	private static final Logger logger = Logger.getLogger(MessageListenerTest.class.getName());

	@Test
	void messageListenerTest() throws Exception {
		var random = new Random();

		var taskExecutorFactory = new ThreadPoolExecutorFactoryBean();
		taskExecutorFactory.setCorePoolSize(5);
		taskExecutorFactory.setQueueCapacity(1);
		taskExecutorFactory.setMaxPoolSize(10);
		taskExecutorFactory.afterPropertiesSet();
		var executor = taskExecutorFactory.getObject();

		var mockMessage = mock(Message.class);

		var consumerMock = mock(MessageConsumer.class);
		Mockito.when(consumerMock.receive(anyLong())).thenAnswer((invocation) -> {
			Thread.sleep(20);
			return (Math.abs(random.nextGaussian()) < 0.4) ? mockMessage : null;
		});

		var sessionMock = mock(Session.class);
		when(sessionMock.createConsumer(any(), any())).thenReturn(consumerMock);

		var connectionMock = mock(Connection.class);
		when(connectionMock.createSession(anyBoolean(), anyInt())).thenReturn(sessionMock);

		var connectionFactoryMock = mock(ConnectionFactory.class);
		when(connectionFactoryMock.createConnection()).thenReturn(connectionMock);

		var messageListener = new MessageListener() {

			@Override
			public void onMessage(Message message) {
				logger.info("received message");
				var sleepTime = random.nextInt(100);
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};

		var container = new DefaultMessageListenerContainer();
		container.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
		container.setConcurrentConsumers(1);
		container.setMaxConcurrentConsumers(10);
		container.setMaxMessagesPerTask(1);
		container.setTaskExecutor(executor);
		container.setConnectionFactory(connectionFactoryMock);
		container.setDestinationName("destination");
		container.setMessageListener(messageListener);
		container.afterPropertiesSet();
		container.start();

		Thread.sleep(Long.MAX_VALUE);
	}

}
