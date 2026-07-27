package com.example.live_classes_service.service.impl;

import com.example.live_classes_service.exception.S3UploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:240}")
    private int expiryMinutes;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${cloud.aws.cloudfront.key-pair-id:}")
    private String cloudFrontKeyPairId;

    @Value("${cloud.aws.cloudfront.private-key-path:}")
    private String cloudFrontPrivateKeyPath;

    public S3Service(S3Client s3Client, S3Presigner presigner) {
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    /**
     * Downloads a recording from the given URL, uploads it to S3,
     * and returns a time-limited presigned URL for secure access.
     */
    public String uploadRecording(String recordingUrl, String sessionId) {
        Path tempFile = null;
        try {
            // Download to a temp file first to get the actual file size
            // (inputStream.available() only returns buffered bytes, not total size)
            tempFile = Files.createTempFile("recording-", ".mp4");
            try (InputStream inputStream = URI.create(recordingUrl).toURL().openStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long fileSize = Files.size(tempFile);
            String key = "recordings/" + sessionId + ".mp4";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("video/mp4")
                    .contentLength(fileSize)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(tempFile));

            log.info("Recording uploaded to S3: {} ({} MB)", key, fileSize / 1024 / 1024);

            // Return a secure presigned URL instead of a direct S3 URL
            return generatePresignedUrl(key);

        } catch (Exception e) {
            throw new S3UploadException("Failed to upload recording to S3", e);
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * Generates a time-limited presigned URL for securely accessing an S3 object.
     * URL expires after the configured expiry time (default 240 minutes).
     */
    public String generatePresignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }

        if (cloudFrontDomain != null && !cloudFrontDomain.isEmpty()
                && cloudFrontKeyPairId != null && !cloudFrontKeyPairId.isEmpty()
                && cloudFrontPrivateKeyPath != null && !cloudFrontPrivateKeyPath.isEmpty()) {
            try {
                String resourceUrl = "https://" + cloudFrontDomain + "/" + key;
                java.time.Instant expiration = java.time.Instant.now().plus(Duration.ofMinutes(expiryMinutes));
                java.security.PrivateKey privateKey = getCloudFrontPrivateKey();

                software.amazon.awssdk.services.cloudfront.CloudFrontUtilities cloudFrontUtilities =
                        software.amazon.awssdk.services.cloudfront.CloudFrontUtilities.create();
                software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest cannedSignerRequest =
                        software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest.builder()
                                .resourceUrl(resourceUrl)
                                .privateKey(privateKey)
                                .keyPairId(cloudFrontKeyPairId)
                                .expirationDate(expiration)
                                .build();

                software.amazon.awssdk.services.cloudfront.url.SignedUrl signedUrl =
                        cloudFrontUtilities.getSignedUrlWithCannedPolicy(cannedSignerRequest);
                log.info("[CLOUDFRONT] Signed URL generated for recording key: {}", key);
                return signedUrl.url();
            } catch (Exception e) {
                log.error("Failed to generate CloudFront signed URL for recording key {}: {}. Falling back to S3.", key, e.getMessage());
            }
        }

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(r -> r.bucket(bucketName).key(key))
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            return null;
        }
    }

    private java.security.PrivateKey getCloudFrontPrivateKey() throws java.io.IOException, java.security.GeneralSecurityException {
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource(cloudFrontPrivateKeyPath);
        if (!resource.exists()) {
            throw new java.io.FileNotFoundException("CloudFront private key not found in classpath at: " + cloudFrontPrivateKeyPath);
        }

        try (java.io.InputStream is = resource.getInputStream()) {
            String pem = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                     .replace("-----END PRIVATE KEY-----", "")
                     .replaceAll("\\s+", "");
            byte[] encoded = java.util.Base64.getDecoder().decode(pem);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
    }
}