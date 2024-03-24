package edu.mirea.vitality.blog.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @file CloudStorageConfig.java
 * @brief Этот файл содержит класс CloudStorageConfig.
 */

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

/**
 * @class CloudStorageConfig
 * @brief Класс конфигурации облачного хранилища.
 */
@Configuration
@Getter
@Component
@RequiredArgsConstructor
public class CloudStorageConfig {
    @Value("${storage.cloud.access.key}")
    public String accessKey;

    @Value("${storage.cloud.secret.key}")
    public String secretKey;

    @Value("${storage.cloud.bucket.name}")
    public String bucketName;

    @Value("${storage.cloud.endpoint.url}")
    public String endpointUrl;

    @Value("${storage.cloud.endpoint.region}")
    public String region;
}
