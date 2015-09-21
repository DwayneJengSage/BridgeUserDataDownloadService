package org.sagebionetworks.bridge.udd.s3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;

/**
 * Helper class that simplifies reading S3 files. This is generally created by Spring. However, we don't use the
 * Component annotation because there are multiple S3 clients, so there may be multiple S3 helpers.
 */
// TODO: This is copy-pasted from BridgePF. We should refactor this into a common shared lib
public class S3Helper {
    private AmazonS3Client s3Client;

    /**
     * S3 Client. This is configured by Spring. We don't use the Autowired annotation because there are multiple S3
     * clients.
     */
    public final void setS3Client(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Pass through to S3 generate presigned URL. This exists mainly as a convenience, so we can do all S3 operations
     * through the helper instead of using the S3 client directly for some operations. This also enables us to add
     * retry logic later.
     *
     * @param bucket
     *         bucket containing the file we want to get a pre-signed URL for
     * @param key
     *         key (filename) to get the pre-signed URL for
     * @param expiration
     *         expiration date of the pre-signed URL
     * @param httpMethod
     *         HTTP method to restrict our pre-signed URL
     * @return the generated pre-signed URL
     */
    public URL generatePresignedUrl(String bucket, String key, DateTime expiration, HttpMethod httpMethod) {
        return s3Client.generatePresignedUrl(bucket, key, expiration.toDate(), httpMethod);
    }

    /**
     * Read the given S3 file as a byte array in memory.
     *
     * @param bucket
     *         S3 bucket to read from, must be non-null and non-empty
     * @param key
     *         S3 key (filename), must be non-null and non-empty
     * @return the S3 file contents as an in-memory byte array
     * @throws IOException
     *         if closing the stream fails
     */
    public byte[] readS3FileAsBytes(String bucket, String key) throws IOException {
        S3Object s3File = s3Client.getObject(bucket, key);
        try (InputStream s3Stream = s3File.getObjectContent()) {
            return ByteStreams.toByteArray(s3Stream);
        }
    }

    /**
     * Read the given S3 file contents as a string. The encoding is assumed to be UTF-8.
     *
     * @param bucket
     *         S3 bucket to read from, must be non-null and non-empty
     * @param key
     *         S3 key (filename), must be non-null and non-empty
     * @return the S3 file contents as a string
     * @throws IOException
     *         if closing the stream fails
     */
    public String readS3FileAsString(String bucket, String key) throws IOException {
        byte[] bytes = readS3FileAsBytes(bucket, key);
        return new String(bytes, Charsets.UTF_8);
    }

    /**
     * Upload the given bytes as an S3 file to S3.
     *
     * @param bucket
     *         bucket to upload to
     * @param key
     *         key (filename) to upload to
     * @param data
     *         bytes to upload
     * @throws IOException
     *         if uploading the byte stream fails
     */
    public void writeBytesToS3(String bucket, String key, byte[] data) throws IOException {
        try (InputStream dataInputStream = new ByteArrayInputStream(data)) {
            s3Client.putObject(bucket, key, dataInputStream, null);
        }
    }

    /**
     * Pass through to S3 PutObject. This exists mainly as a convenience, so we can do all S3 operations through the
     * helper instead of using the S3 client directly for some operations. This also enables us to add retry logic
     * later.
     *
     * @param bucket
     *         bucket to upload to
     * @param key
     *         key (filename) to upload to
     * @param file
     *         file to upload
     */
    public void writeFileToS3(String bucket, String key, File file) {
        s3Client.putObject(bucket, key, file);
    }
}
