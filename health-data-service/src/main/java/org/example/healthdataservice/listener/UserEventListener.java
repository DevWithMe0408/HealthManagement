package org.example.healthdataservice.listener;

import org.example.events.UserCreatedEvent;
import org.example.events.UserProfileUpdatedEvent;
import org.example.healthdataservice.service.CalculatedMetricService;
import org.example.healthdataservice.service.HealthIndicatorConfigsService;
import org.example.healthdataservice.service.UserProfileMirrorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UserEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private HealthIndicatorConfigsService healthIndicatorConfigsService;

    @Autowired
    private CalculatedMetricService calculatedMetricService;

    @Autowired
    private UserProfileMirrorService userProfileMirrorService;


    // Lắng nghe trên queue đã được cấu hình trong HealthDataRabbitMQConfig
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
            userProfileMirrorService.createDefaultUserForHealthData(userId);
            log.info("Successfully created default UserForHealthData for userId: {}", userId);
            calculatedMetricService.recalculateAllDerivedMetricsForUser(userId);


            log.info("Successfully processed UserCreatedEvent for userId: {}", userId);
        }catch (NumberFormatException e) {
            log.error("Error parsing userId '{}' from UserCreatedEvent: {}",event.getUserId(), e.getMessage());
            throw new RuntimeException("Invalid userId format in UserCreatedEvent: " + event.getUserId(), e);
        } catch (Exception e) {
            log.error("Error processing UserCreatedEvent for userId {}: {}", event.getUserId(), e.getMessage(),e);
            throw new RuntimeException("Failed to process UserCreatedEvent for userId: {}" + event.getUserId(), e);
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.health-data-user-profile-updated}")
    public void handleUserProfileUpdatedEvent(@Payload UserProfileUpdatedEvent event) {
        log.info("Health-Data-Service: Received UserProfileUpdatedEvent for userId: {}, birthDate: {}, gender: {}",
                event.getUserId(),event.getBirthDate(), event.getGender());

        try {
            Long userId = Long.parseLong(event.getUserId());
            LocalDate birthDate = event.getBirthDate();
            String gender = event.getGender();

            // Bước 1: Lưu trữ/Cập nhật thông tin profile này trong Health-Data-Service

             userProfileMirrorService.saveOrUpdateUserProfile(userId, birthDate, gender);

            log.info("TODO: Implement logic to store/update user profile (birthDate, gender) for userId {} " +
                    "in Health-Data-Service's own storage.", userId);

            // Bước 2: Tính toán lại tất cả các chỉ số dẫn xuất vì profile đã thay đổi
            calculatedMetricService.recalculateAllDerivedMetricsForUser(userId);
        } catch (NumberFormatException e) {
            log.error("Error parsing userId '{}' from UserProfileUpdatedEvent: {}", event.getUserId(), e.getMessage());
            throw new RuntimeException("Invalid userId format in UserProfileUpdatedEvent: " + event.getUserId(), e);
        } catch (Exception e) {
            log.error("Error processing UserProfileUpdatedEvent for userId {}: {}", event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process UserProfileUpdatedEvent for userId: " + event.getUserId(), e);
        }
    }


}
