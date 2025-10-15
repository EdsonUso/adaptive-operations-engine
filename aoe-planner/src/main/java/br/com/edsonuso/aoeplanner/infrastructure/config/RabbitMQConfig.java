package br.com.edsonuso.aoeplanner.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "aoe.plans.exchange";
    public static final String QUEUE_NAME = "aoe.plan.queue";
    public static final String ROUTING_KEY = "plan.new";

    public static final String REPLAN_EXCHANGE_NAME = "aoe.replan.exchange";
    public static final String REPLAN_QUEUE_NAME = "aoe.replan.queue";
    public static final String REPLAN_ROUTING_KEY = "replan.request";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public TopicExchange replanExchange() {
        return new TopicExchange(REPLAN_EXCHANGE_NAME);
    }

    @Bean
    public Queue replanQueue() {
        return new Queue(REPLAN_QUEUE_NAME);
    }

    @Bean
    public Binding replanBinding(Queue replanQueue, TopicExchange replanExchange) {
        return BindingBuilder.bind(replanQueue).to(replanExchange).with(REPLAN_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
