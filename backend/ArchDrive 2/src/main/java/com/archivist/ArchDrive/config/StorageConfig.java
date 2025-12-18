package com.archivist.ArchDrive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import java.net.URI;
import java.time.Duration;

@Configuration
public class StorageConfig {

    @Value("${cloudflare.r2.accountId}")
    private String accountId;

    @Value("${cloudflare.r2.accessKey}")
    private String accessKey;

    @Value("${cloudflare.r2.secretKey}")
    private String secretKey;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        // R2 вимагає path-style запити (bucket у шляху, а не в хості)
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                // R2 не підтримує перевірку контрольних сум AWS SDK, тому вимикаємо
                .checksumValidationEnabled(false)
                // Вимикаємо chunked encoding, щоб уникнути Broken pipe
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .serviceConfiguration(s3Config)
                .httpClientBuilder(
                        ApacheHttpClient.builder()
                                .maxConnections(100)
                                .connectionTimeout(Duration.ofSeconds(30))
                                .socketTimeout(Duration.ofSeconds(60))
                                .expectContinueEnabled(false)
                )
                .overrideConfiguration(b -> b
                        .apiCallTimeout(java.time.Duration.ofMinutes(20))  // Збільшено до 20 хвилин
                        .apiCallAttemptTimeout(java.time.Duration.ofMinutes(20))
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultStrategy())
                                .numRetries(15) // Збільшено до 15 спроб для мобільного інтернету
                                .build())
                )
                // Для R2 рекомендують US_EAST_1 у підписі
                .region(Region.US_EAST_1)
                .build();
    }
}

