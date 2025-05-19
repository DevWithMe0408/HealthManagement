package org.example.healthdataservice.listener;

import org.example.events.UserCreatedEvent;
import org.example.healthdataservice.service.HealthIndicatorConfigsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private HealthIndicatorConfigsService healthIndicatorConfigsService;

    // Lắng nghe trên queue đã được cấu hình trong HealthDataRabbitMQConfig
    // Hoặc bạn có thể khai báo queue trực tiếp tại đây nếu muốn
    @RabbitListener(queues = "${app.rabbitmq.queue.health-data-user-created}")
    public void handleUserCreatedEvent(@Payload UserCreatedEvent event) {
        log.info("Health-Data-Service: Received UserCreatedEvent for userId: {}, username: {}",
                event.getUserId(), event.getUsername());

        try {
            // Chuyển đổi userId từ String (trong event) sang Long (trong entity)
            Long userId = Long.parseLong(event.getUserId());

            // Gọi service để tạo các bản ghi default
            healthIndicatorConfigsService.createDefaultHealthIndicatorConfigsForUser(userId);
            log.info("Successfully created default health indicator configs for userId: {}", userId);
        }catch (NumberFormatException e) {
            log.error("Error parsing userId '{}' from UserCreatedEvent: {}",event.getUserId(), e.getMessage());
            // Quyết định cách xử lý: nack, gửi tới DLQ, hoặc log và bỏ qua
            // Ném exception sẽ khiến message được nack và có thể retry hoặc vào DLQ (tùy cấu hình)
            throw new RuntimeException("Invalid userId format in UserCreatedEvent: " + event.getUserId(), e);
        } catch (Exception e) {
            log.error("Error processing UserCreatedEvent for userId {}: {}", event.getUserId(), e.getMessage(),e);
            // Ném một exception để Spring AMQP biết xử lý message thất bại
            // và có thể retry hoặc gửi tới DLQ (Dead Letter Queue).
            throw new RuntimeException("Failed to process UserCreatedEvent for userId: {}" + event.getUserId(), e);
        }
    }
}
