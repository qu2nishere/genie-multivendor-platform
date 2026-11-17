package com.genie.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Tên bucket thật và URL công khai cho Silo (máy dev) và AWS S3 (production) — ADR 0010. */
class S3ObjectStorageTest {

    private static S3ObjectStorage storage(String endpoint, String publicUrl, String region, String prefix) {
        StorageProperties p = new StorageProperties(endpoint, publicUrl, "key", "secret",
                Duration.ofMinutes(15), Duration.ofMinutes(10), Duration.ofMinutes(5), region, prefix);
        return new S3ObjectStorage(p, Clock.systemUTC());
    }

    @Test
    void siloKeepsPathStyleUrlAndLogicalBucketWhenNoPrefix() {
        S3ObjectStorage s = storage("http://localhost:9000", "http://localhost:9000/", "us-east-1", "");

        assertThat(s.publicUrl("avatar-assets", "user/1/a.jpg"))
                .isEqualTo("http://localhost:9000/avatar-assets/user/1/a.jpg");
    }

    @Test
    void awsUsesVirtualHostUrlWithPrefixedBucket() {
        String aws = "https://s3.ap-southeast-1.amazonaws.com";
        S3ObjectStorage s = storage(aws, aws, "ap-southeast-1", "genie-prod-");

        assertThat(s.publicUrl("avatar-assets", "user/1/a.jpg"))
                .isEqualTo("https://genie-prod-avatar-assets.s3.ap-southeast-1.amazonaws.com/user/1/a.jpg");
    }

    @Test
    void prefixAlsoAppliesToPathStyleStorage() {
        S3ObjectStorage s = storage("http://localhost:9000", "http://localhost:9000", "us-east-1", "test-");

        assertThat(s.publicUrl("product-media", "x.webp")).isEqualTo("http://localhost:9000/test-product-media/x.webp");
    }

    @Test
    void detectsAwsHostsOnly() {
        assertThat(S3ObjectStorage.isAwsHost("https://s3.ap-southeast-1.amazonaws.com")).isTrue();
        assertThat(S3ObjectStorage.isAwsHost("https://s3.amazonaws.com")).isTrue();
        assertThat(S3ObjectStorage.isAwsHost("http://localhost:9000")).isFalse();
        assertThat(S3ObjectStorage.isAwsHost("https://s3.genie.vn")).isFalse();
        assertThat(S3ObjectStorage.isAwsHost("https://notamazonaws.com.evil.vn")).isFalse();
    }

    @Test
    void physicalBucketJoinsPrefix() {
        StorageProperties p = new StorageProperties("http://x", "http://x", null, null,
                Duration.ofMinutes(1), Duration.ofMinutes(1), Duration.ofMinutes(1), "us-east-1", "genie-prod-");

        assertThat(p.physicalBucket("kyc-dossiers")).isEqualTo("genie-prod-kyc-dossiers");
    }
}
