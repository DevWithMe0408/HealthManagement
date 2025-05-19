package org.example.healthdataservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthDataRabbitMQConfig {

    @Value("${app.rabbitmq.exchange.user-events}")
    private String userEventsExchangeName;

    @Value("${app.rabbitmq.queue.health-data-user-created}")
    private String userCreatedQueueName;

    @Value("${app.rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    // 1. Định nghĩa Queue cho service này để nhận UserCreatedEvent
    @Bean
    Queue healthDataUserCreatedQueue() {
        // Tham số: name, durable (true), exclusive (false), autoDelete (false)
        return new Queue(userCreatedQueueName, true, false, false);
    }

    // 2. Khai báo lại Exchange (để đảm bảo nó tồn tại)
    // Các thuộc tính phải khớp với khai báo ở User Service (Producer).
    @Bean
    TopicExchange userEventsExchange() {
        return new TopicExchange(userEventsExchangeName, true, false);
    }

    // 3. Tạo Binding giữa Queue và Exchange với Routing Key
    @Bean
    Binding healthDataUserCreatedBinding(Queue healthDataUserCreatedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(healthDataUserCreatedQueue)
                .to(userEventsExchange)
                .with(userCreatedRoutingKey); // Chỉ nhận message có routing key này
    }

    // 4. Định nghĩa MessageConverter (phải giống với User Service)
    // Điều này đảm bảo Spring AMQP có thể deserialize message JSON thành UserCreatedEvent object.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    // 5. (Tùy chọn) Cấu hình RabbitTemplate nếu Health-Data-Service cũng cần GỬI message.
    // Trong trường hợp này, nó chủ yếu là consumer, nên không bắt buộc.
    // Spring Boot sẽ tự động cấu hình listener để sử dụng MessageConverter đã định nghĩa ở trên.
}
