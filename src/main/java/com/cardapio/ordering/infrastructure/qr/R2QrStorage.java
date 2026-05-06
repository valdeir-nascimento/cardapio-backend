package com.cardapio.ordering.infrastructure.qr;

import com.cardapio.ordering.domain.port.QrStorage;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true")
public class R2QrStorage implements QrStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final R2Properties props;

    public R2QrStorage(S3Client s3, S3Presigner presigner, R2Properties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.props = props;
    }

    @Override
    @Retry(name = "r2")
    public void putIfAbsent(String key, byte[] bytes, String contentType) {
        if (objectExists(key)) {
            return;
        }
        s3.putObject(PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes));
    }

    @Override
    @Retry(name = "r2")
    public URI presignedUrl(String key, Duration ttl) {
        var getReq = GetObjectRequest.builder().bucket(props.bucket()).key(key).build();
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(getReq)
            .build());
        return presigned.url().toString().transform(URI::create);
    }

    private boolean objectExists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(props.bucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
