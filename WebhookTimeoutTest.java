import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Test suite for webhook timeout handling
 * Tests various timeout scenarios to ensure proper error handling
 */
public class WebhookTimeoutTest {

    public static void main(String[] args) {
        WebhookTimeoutTest test = new WebhookTimeoutTest();
        int passed = 0;
        int failed = 0;

        System.out.println("=== Webhook Timeout Tests ===\n");

        // Test 1: Fast processing should complete successfully
        if (test.testFastProcessingCompletes()) {
            System.out.println("✓ Test 1 PASSED: Fast processing completes");
            passed++;
        } else {
            System.out.println("✗ Test 1 FAILED: Fast processing completes");
            failed++;
        }

        // Test 2: Slow processing should timeout
        if (test.testSlowProcessingTimesOut()) {
            System.out.println("✓ Test 2 PASSED: Slow processing times out");
            passed++;
        } else {
            System.out.println("✗ Test 2 FAILED: Slow processing times out");
            failed++;
        }

        // Test 3: Timeout response includes proper error details
        if (test.testTimeoutResponseFormat()) {
            System.out.println("✓ Test 3 PASSED: Timeout response format is correct");
            passed++;
        } else {
            System.out.println("✗ Test 3 FAILED: Timeout response format is correct");
            failed++;
        }

        // Test 4: Multiple concurrent requests handle timeouts independently
        if (test.testConcurrentTimeouts()) {
            System.out.println("✓ Test 4 PASSED: Concurrent timeouts handled independently");
            passed++;
        } else {
            System.out.println("✗ Test 4 FAILED: Concurrent timeouts handled independently");
            failed++;
        }

        // Test 5: Timeout value is respected
        if (test.testCustomTimeoutValue()) {
            System.out.println("✓ Test 5 PASSED: Custom timeout value is respected");
            passed++;
        } else {
            System.out.println("✗ Test 5 FAILED: Custom timeout value is respected");
            failed++;
        }

        // Test 6: Max timeout cap is enforced
        if (test.testMaxTimeoutCap()) {
            System.out.println("✓ Test 6 PASSED: Max timeout cap is enforced");
            passed++;
        } else {
            System.out.println("✗ Test 6 FAILED: Max timeout cap is enforced");
            failed++;
        }

        // Test 7: Valid signature with fast processing
        if (test.testValidSignatureProcessing()) {
            System.out.println("✓ Test 7 PASSED: Valid signature processes successfully");
            passed++;
        } else {
            System.out.println("✗ Test 7 FAILED: Valid signature processes successfully");
            failed++;
        }

        // Test 8: Invalid signature is rejected before timeout check
        if (test.testInvalidSignatureRejected()) {
            System.out.println("✓ Test 8 PASSED: Invalid signature rejected");
            passed++;
        } else {
            System.out.println("✗ Test 8 FAILED: Invalid signature rejected");
            failed++;
        }

        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));

        System.exit(failed > 0 ? 1 : 0);
    }

    /**
     * Test that fast processing completes successfully without timeout
     */
    public boolean testFastProcessingCompletes() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0); // No delay
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"transaction_id\":\"tx_123\",\"amount\":\"99.99\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_fast_1");

        controller.shutdown();

        return (Integer) response.get("status") == 200
            && !response.containsKey("timeout");
    }

    /**
     * Test that slow processing triggers timeout
     */
    public boolean testSlowProcessingTimesOut() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(10000); // 10 second delay
        WebhookController controller = new WebhookController(1000, handler); // 1 second timeout

        String payload = "{\"transaction_id\":\"tx_slow\",\"amount\":\"50.00\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        long startTime = System.currentTimeMillis();
        Map<String, Object> response = controller.processWebhook(payload, signature, "req_slow_1");
        long duration = System.currentTimeMillis() - startTime;

        controller.shutdown();

        return (Integer) response.get("status") == 408
            && response.containsKey("timeout")
            && (Boolean) response.get("timeout") == true
            && duration < 2000; // Should timeout well before the 10s delay
    }

    /**
     * Test timeout response includes proper error details
     */
    public boolean testTimeoutResponseFormat() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(5000);
        WebhookController controller = new WebhookController(500, handler);

        String payload = "{\"transaction_id\":\"tx_format\",\"amount\":\"75.00\",\"currency\":\"EUR\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_format_1");

        controller.shutdown();

        return response.containsKey("status")
            && (Integer) response.get("status") == 408
            && response.containsKey("error")
            && ((String) response.get("error")).contains("timeout")
            && response.containsKey("requestId")
            && "req_format_1".equals(response.get("requestId"))
            && response.containsKey("timeout");
    }

    /**
     * Test that concurrent requests handle timeouts independently
     */
    public boolean testConcurrentTimeouts() {
        PaymentWebhookHandler fastHandler = new PaymentWebhookHandler(0);
        PaymentWebhookHandler slowHandler = new PaymentWebhookHandler(5000);

        WebhookController fastController = new WebhookController(2000, fastHandler);
        WebhookController slowController = new WebhookController(500, slowHandler);

        String fastPayload = "{\"transaction_id\":\"tx_fast\",\"amount\":\"10.00\",\"currency\":\"USD\"}";
        String slowPayload = "{\"transaction_id\":\"tx_slow\",\"amount\":\"20.00\",\"currency\":\"USD\"}";

        String fastSig = generateValidSignature(fastPayload);
        String slowSig = generateValidSignature(slowPayload);

        // Start both requests concurrently
        Thread fastThread = new Thread(() -> {
            fastController.processWebhook(fastPayload, fastSig, "req_concurrent_fast");
        });

        Thread slowThread = new Thread(() -> {
            slowController.processWebhook(slowPayload, slowSig, "req_concurrent_slow");
        });

        fastThread.start();
        slowThread.start();

        try {
            fastThread.join();
            slowThread.join();
        } catch (InterruptedException e) {
            return false;
        }

        fastController.shutdown();
        slowController.shutdown();

        // Both handlers should process independently
        return fastHandler.getConfirmationCount() == 1
            && slowHandler.getConfirmationCount() == 0; // Slow one should timeout
    }

    /**
     * Test that custom timeout value is respected
     */
    public boolean testCustomTimeoutValue() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(3000);
        WebhookController controller = new WebhookController(4000, handler);

        String payload = "{\"transaction_id\":\"tx_custom\",\"amount\":\"100.00\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        long startTime = System.currentTimeMillis();
        Map<String, Object> response = controller.processWebhook(payload, signature, "req_custom_1");
        long duration = System.currentTimeMillis() - startTime;

        controller.shutdown();

        // Should complete successfully since handler delay (3s) < timeout (4s)
        return (Integer) response.get("status") == 200
            && duration >= 3000
            && duration < 5000
            && controller.getTimeoutMs() == 4000;
    }

    /**
     * Test that max timeout cap is enforced
     */
    public boolean testMaxTimeoutCap() {
        WebhookController controller = new WebhookController(60000); // Try to set 60 seconds

        // Should be capped at 30 seconds (MAX_TIMEOUT_MS)
        boolean capEnforced = controller.getTimeoutMs() == 30000;

        controller.shutdown();

        return capEnforced;
    }

    /**
     * Test valid signature with fast processing
     */
    public boolean testValidSignatureProcessing() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"transaction_id\":\"tx_sig_valid\",\"amount\":\"150.00\",\"currency\":\"GBP\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_sig_valid");

        controller.shutdown();

        return (Integer) response.get("status") == 200
            && handler.getConfirmationCount() == 1
            && handler.getConfirmation("tx_sig_valid") != null;
    }

    /**
     * Test invalid signature is rejected
     */
    public boolean testInvalidSignatureRejected() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"transaction_id\":\"tx_sig_invalid\",\"amount\":\"200.00\",\"currency\":\"CAD\"}";
        String invalidSignature = "sha256=invalid_signature_hash";

        Map<String, Object> response = controller.processWebhook(payload, invalidSignature, "req_sig_invalid");

        controller.shutdown();

        return (Integer) response.get("status") == 401
            && ((String) response.get("error")).contains("Invalid signature")
            && handler.getConfirmationCount() == 0; // Should not process
    }

    /**
     * Generate a valid signature for testing
     * This matches the signature generation in WebhookController
     */
    private String generateValidSignature(String payload) {
        try {
            String secret = System.getenv("WEBHOOK_SECRET");
            if (secret == null || secret.isEmpty()) {
                secret = "default-secret-key-change-in-production";
            }

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);

        } catch (Exception e) {
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
