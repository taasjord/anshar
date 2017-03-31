package no.rutebanken.anshar.routes.health;

import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IMap;
import no.rutebanken.anshar.messages.collections.HealthCheckKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Configuration
public class HealthManager {

    private Logger logger = LoggerFactory.getLogger(HealthManager.class);

    @Autowired
    @Qualifier("getHealthCheckMap")
    private IMap<Enum<HealthCheckKey>, Instant> healthCheckMap;


    @Value("${anshar.admin.health.allowed.inactivity.seconds:999}")
    private long allowedInactivityTime;

    @Value("${anshar.healthcheck.interval.seconds}")
    private int healthCheckInterval = 30;

    public boolean isHazelcastAlive() {
        try {
            healthCheckMap.put(HealthCheckKey.NODE_LIVENESS_CHECK, Instant.now());
            return healthCheckMap.containsKey(HealthCheckKey.NODE_LIVENESS_CHECK);
        } catch (HazelcastInstanceNotActiveException e) {
            logger.warn("HazelcastInstance not active - ", e);
            return false;
        }
    }

    @Bean
    public Instant serverStartTime() {
        if (!healthCheckMap.containsKey(HealthCheckKey.SERVER_START_TIME)) {
            healthCheckMap.put(HealthCheckKey.SERVER_START_TIME, Instant.now());
        }
        return healthCheckMap.get(HealthCheckKey.SERVER_START_TIME);
    }

    public void dataReceived() {
        healthCheckMap.put(HealthCheckKey.HEALTH_CHECK_INCOMING_DATA, Instant.now());
    }


    public boolean isReceivingData() {
        Instant lastReceivedData = healthCheckMap.get(HealthCheckKey.HEALTH_CHECK_INCOMING_DATA);
        if (lastReceivedData != null) {
            long lastReceivedMillis = lastReceivedData.toEpochMilli();

            long seconds = (Instant.now().toEpochMilli() - lastReceivedMillis) / (1000);
            if (seconds > allowedInactivityTime) {
                logger.warn("Last received data: {}, {} seconds ago", lastReceivedData, seconds);
                return false;
            }
        }
        return true;
    }

    public long getSecondsSinceDataReceived() {
        Instant lastReceivedData = healthCheckMap.get(HealthCheckKey.HEALTH_CHECK_INCOMING_DATA);
        if (lastReceivedData != null) {
            long lastReceivedMillis = lastReceivedData.toEpochMilli();

            long seconds = (Instant.now().toEpochMilli() - lastReceivedMillis) / (1000);
            return seconds;
        }
        return -1;
    }

    public boolean isHealthCheckRunning() {
        Instant lastHealthCheck = healthCheckMap.get(HealthCheckKey.ANSHAR_HEALTHCHECK_KEY);
        if (lastHealthCheck != null) {
            long lastHealthCheckMillis = lastHealthCheck.toEpochMilli();

            long seconds = (Instant.now().toEpochMilli() - lastHealthCheckMillis) / (1000);
            if (seconds > healthCheckInterval * 3) {
                logger.warn("Last healthCheck: {}, {} seconds ago", lastHealthCheckMillis, seconds);
                return false;
            }
        }
        return true;
    }
}