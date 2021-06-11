package org.geekbang.time.commonmistakes.asyncprocess.deadletter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
@Slf4j
public class RabbitConfiguration {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Bean
    public Declarables declarables() {
        Queue queue = new Queue(Consts.QUEUE);
        DirectExchange directExchange = new DirectExchange(Consts.EXCHANGE);
        return new Declarables(queue, directExchange,
                BindingBuilder.bind(queue).to(directExchange).with(Consts.ROUTING_KEY));
    }

    @Bean
    public Declarables declarablesForDead() {
        // 死信队列 绑定死信交换机
        Queue queue = new Queue(Consts.DEAD_QUEUE);
        DirectExchange directExchange = new DirectExchange(Consts.DEAD_EXCHANGE);
        return new Declarables(queue, directExchange,
                BindingBuilder.bind(queue).to(directExchange).with(Consts.DEAD_ROUTING_KEY));
    }

    // 定义重试操作拦截器
    @Bean
    public RetryOperationsInterceptor interceptor() {
        return RetryInterceptorBuilder.stateless()
                // 最大尝试次数
                .maxAttempts(5)
                // 指数退避重试
                .backOffOptions(1000, 2.0, 10000)
                // 重试4次后 , 推送消息到死信队列
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, Consts.DEAD_EXCHANGE, Consts.DEAD_ROUTING_KEY))
                .build();
    }

    // 增加消费线程
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(interceptor());
        factory.setConcurrentConsumers(10);
        return factory;
    }
}
