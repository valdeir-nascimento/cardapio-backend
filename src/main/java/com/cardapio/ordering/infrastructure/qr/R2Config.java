package com.cardapio.ordering.infrastructure.qr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
@ConditionalOnProperty(prefix = "r2", name = "endpoint")
public class R2Config {

    @Bean(destroyMethod = "close")
    S3Client r2S3Client(R2Properties props) {
        return S3Client.builder()
            .endpointOverride(URI.create(props.endpoint()))
            .region(Region.of(props.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKeyId(), props.secretAccessKey())))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner r2S3Presigner(R2Properties props) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(props.endpoint()))
            .region(Region.of(props.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKeyId(), props.secretAccessKey())))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();
    }
}
