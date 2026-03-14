import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * WebhookController handles incoming webhook requests with timeout protection
 * and signature validation.
 */
public class WebhookController {
    private static final int DEFAULT_TIMEOUT_MS = 5000; // 5 seconds
    private static final int MAX_TIMEOUT_MS = 30000; // 30 seconds
    private final ExecutorService executorService;
    private final int timeoutMs;
    private final PaymentWebhookHandler webhookHandler;

    public WebhookController() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public WebhookController(int timeoutMs) {
        this.timeoutMs = Math.min(timeoutMs, MAX_TIMEOUT_MS);
        this.executorService = Executors.newCachedThreadPool();
        this.webhookHandler = new PaymentWebhookHandler();
    }

    public WebhookController(int timeoutMs, PaymentWebhookHandler handler) {
        this.timeoutMs = Math.min(timeoutMs, MAX_TIMEOUT_MS);
        this.executorService = Executors.newCachedThreadPool();
        this.webhookHandler = handler;
    }

    /**
     * Process webhook request with timeout protection
     * @param payload The webhook payload
     * @param signature The signature from the webhook provider
     * @param requestId Unique request identifier for tracking
     * @return Response map with status and message
     */
    public Map<String, Object> processWebhook(String payload, String signature, String requestId) {
        Map<String, Object> response = new HashMap<>();

        Future<Map<String, Object>> future = executorService.submit(() -> {
            try {
                // Validate signature
                if (!validateSignature(payload, signature)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", 401);
                    errorResponse.put("error", "Invalid signature verification");
                    errorResponse.put("requestId", requestId);
                    return errorResponse;
                }

                // Process the webhook
                return webhookHandler.process(payload, requestId);

            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", 500);
                errorResponse.put("error", "Internal processing error: " + e.getMessage());
                errorResponse.put("requestId", requestId);
                return errorResponse;
            }
        });

        try {
            // Wait for processing with timeout
            response = future.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            future.cancel(true);
            response.put("status", 408);
            response.put("error", "Webhook processing timeout after " + timeoutMs + "ms");
            response.put("requestId", requestId);
            response.put("timeout", true);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.put("status", 500);
            response.put("error", "Webhook processing interrupted");
            response.put("requestId", requestId);

        } catch (ExecutionException e) {
            response.put("status", 500);
            response.put("error", "Webhook execution failed: " + e.getCause().getMessage());
            response.put("requestId", requestId);
        }

        return response;
    }

    /**
     * Validate webhook signature using HMAC-SHA256
     * @param payload The webhook payload
     * @param receivedSignature The signature from the request header
     * @return true if signature is valid, false otherwise
     */
    public boolean validateSignature(String payload, String receivedSignature) {
        try {
            String secret = getWebhookSecret();

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(hash);

            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedSignature, receivedSignature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Error validating signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get webhook secret from environment or configuration
     * In production, this should read from secure configuration
     */
    private String getWebhookSecret() {
        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null || secret.isEmpty()) {
            secret = "default-secret-key-change-in-production";
        }
        return secret;
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Shutdown the executor service gracefully
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
