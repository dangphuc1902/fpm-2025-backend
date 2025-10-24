package com.fpm_2025.wallet_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String WALLET_EXCHANGE = "wallet.exchange";
    public static final String TRANSACTION_EXCHANGE = "transaction.exchange";

    // Queue names
    public static final String WALLET_CREATED_QUEUE = "wallet.created.queue";
    public static final String WALLET_UPDATED_QUEUE = "wallet.updated.queue";
    public static final String TRANSACTION_CREATED_QUEUE = "transaction.created.queue";
    public static final String BALANCE_UPDATE_QUEUE = "balance.update.queue";

    // Routing keys
    public static final String WALLET_CREATED_KEY = "wallet.created";
    public static final String WALLET_UPDATED_KEY = "wallet.updated";
    public static final String TRANSACTION_CREATED_KEY = "transaction.created";
    public static final String BALANCE_UPDATE_KEY = "balance.update";

    @Bean
    public TopicExchange walletExchange() {
        return new TopicExchange(WALLET_EXCHANGE);
    }

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(TRANSACTION_EXCHANGE);
    }

    @Bean
    public Queue walletCreatedQueue() {
        return QueueBuilder.durable(WALLET_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }

    @Bean
    public Queue walletUpdatedQueue() {
        return QueueBuilder.durable(WALLET_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }

    @Bean
    public Queue transactionCreatedQueue() {
        return QueueBuilder.durable(TRANSACTION_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }

    @Bean
    public Queue balanceUpdateQueue() {
        return QueueBuilder.durable(BALANCE_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }

    @Bean
    public Binding walletCreatedBinding(Queue walletCreatedQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(walletCreatedQueue)
                .to(walletExchange)
                .with(WALLET_CREATED_KEY);
    }

    @Bean
    public Binding walletUpdatedBinding(Queue walletUpdatedQueue, TopicExchange walletExchange) {
        return BindingBuilder.bind(walletUpdatedQueue)
                .to(walletExchange)
                .with(WALLET_UPDATED_KEY);
    }

    @Bean
    public Binding transactionCreatedBinding(Queue transactionCreatedQueue, TopicExchange transactionExchange) {
        return BindingBuilder.bind(transactionCreatedQueue)
                .to(transactionExchange)
                .with(TRANSACTION_CREATED_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
