package com.abc.actuator;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component("version")
public class VersionHealthIndicator implements HealthIndicator {

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Value("${APP_VERSION:unknown}")
    private String appVersion;

    @Value("${APP_IMAGE:unknown}")
    private String appImage;

    public VersionHealthIndicator(ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    @Override
    public Health health() {
        BuildProperties build = buildPropertiesProvider.getIfAvailable();

        Health.Builder builder = Health.up()
                .withDetail("version", appVersion)
                .withDetail("image", appImage);

        if (build != null) {
            builder.withDetail("buildVersion", build.getVersion());
            if (build.getTime() != null) {
                builder.withDetail("buildTime", build.getTime().toString());
            }
        }

        return builder.build();
    }
}
