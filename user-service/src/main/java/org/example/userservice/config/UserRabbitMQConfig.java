package org.example.userservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserRabbitMQConfig {

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    // 1. Định nghĩa Exchange (Producer "sở hữu" việc tạo exchange này)
    @Bean
    TopicExchange userEventsExchange() {
        // Tham số thứ 2: durable (true) - exchange sẽ tồn tại sau khi broker restart
        // Tham số thứ 3: autoDelete (false) - exchange sẽ không bị xóa khi không còn queue nào bind tới
        return new TopicExchange(userEventsExchangeName, true, false);
    }

    // 2. Định nghĩa MessageConverter để Spring tự động chuyển đổi Object sang JSON và ngược lại
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 3. Cấu hình RabbitTemplate để sử dụng MessageConverter ở trên
    // Spring Boot sẽ tự động cấu hình RabbitTemplate, nhưng nếu bạn muốn tùy chỉnh
    // message converter, bạn cần cung cấp bean này.
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
