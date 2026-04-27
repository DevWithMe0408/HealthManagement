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

@Component
public class UserEventListener {
    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private HealthIndicatorConfigsService healthIndicatorConfigsService;

    @Autowired
    private CalculatedMetricService calculatedMetricService;

    @Autowired
    private UserProfileMirrorService userProfileMirrorService;

    @RabbitListener(queues = "${app.rabbitmq.queue.health-data-user-created}")
    public void handleUserCreatedEvent(@Payload UserCreatedEvent event) {
        String userId = event.getUserId();
        log.info("Health-Data-Service: Received UserCreatedEvent for userId: {}, username: {}",
                userId, event.getUsername());
        try {
            healthIndicatorConfigsService.createDefaultHealthIndicatorConfigsForUser(userId);
            log.info("Created default health indicator configs for userId: {}", userId);
            userProfileMirrorService.createDefaultUserForHealthData(userId);
            log.info("Created default UserForHealthData for userId: {}", userId);
            calculatedMetricService.recalculateAllDerivedMetricsForUser(userId);
            log.info("Successfully processed UserCreatedEvent for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error processing UserCreatedEvent for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to process UserCreatedEvent for userId: " + userId, e);
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.health-data-user-profile-updated}")
    public void handleUserProfileUpdatedEvent(@Payload UserProfileUpdatedEvent event) {
        String userId = event.getUserId();
        log.info("Health-Data-Service: Received UserProfileUpdatedEvent for userId: {}, birthDate: {}, gender: {}",
                userId, event.getBirthDate(), event.getGender());
        try {
            userProfileMirrorService.saveOrUpdateUserProfile(userId, event.getBirthDate(), event.getGender());
            calculatedMetricService.recalculateAllDerivedMetricsForUser(userId);
            log.info("Successfully processed UserProfileUpdatedEvent for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error processing UserProfileUpdatedEvent for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to process UserProfileUpdatedEvent for userId: " + userId, e);
        }
    }
}
