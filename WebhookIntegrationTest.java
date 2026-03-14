import java.util.Map;

/**
 * Integration tests for complete webhook processing flow
 * Tests end-to-end scenarios including signature validation,
 * payload processing, and confirmation recording
 */
public class WebhookIntegrationTest {

    public static void main(String[] args) {
        WebhookIntegrationTest test = new WebhookIntegrationTest();
        int passed = 0;
        int failed = 0;

        System.out.println("=== Webhook Integration Tests ===\n");

        // Test 1: Complete successful webhook flow
        if (test.testSuccessfulWebhookFlow()) {
            System.out.println("✓ Test 1 PASSED: Complete successful webhook flow");
            passed++;
        } else {
            System.out.println("✗ Test 1 FAILED: Complete successful webhook flow");
            failed++;
        }

        // Test 2: Invalid payment data is rejected
        if (test.testInvalidPaymentDataRejected()) {
            System.out.println("✓ Test 2 PASSED: Invalid payment data rejected");
            passed++;
        } else {
            System.out.println("✗ Test 2 FAILED: Invalid payment data rejected");
            failed++;
        }

        // Test 3: Duplicate transaction IDs are handled
        if (test.testDuplicateTransactions()) {
            System.out.println("✓ Test 3 PASSED: Duplicate transactions handled");
            passed++;
        } else {
            System.out.println("✗ Test 3 FAILED: Duplicate transactions handled");
            failed++;
        }

        // Test 4: Multiple successful webhooks are recorded
        if (test.testMultipleWebhooks()) {
            System.out.println("✓ Test 4 PASSED: Multiple webhooks recorded");
            passed++;
        } else {
            System.out.println("✗ Test 4 FAILED: Multiple webhooks recorded");
            failed++;
        }

        // Test 5: Webhook with missing fields
        if (test.testMissingFields()) {
            System.out.println("✓ Test 5 PASSED: Missing fields handled correctly");
            passed++;
        } else {
            System.out.println("✗ Test 5 FAILED: Missing fields handled correctly");
            failed++;
        }

        // Test 6: Different currencies are supported
        if (test.testMultipleCurrencies()) {
            System.out.println("✓ Test 6 PASSED: Multiple currencies supported");
            passed++;
        } else {
            System.out.println("✗ Test 6 FAILED: Multiple currencies supported");
            failed++;
        }

        // Test 7: Large transaction amounts
        if (test.testLargeAmounts()) {
            System.out.println("✓ Test 7 PASSED: Large amounts handled correctly");
            passed++;
        } else {
            System.out.println("✗ Test 7 FAILED: Large amounts handled correctly");
            failed++;
        }

        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));

        System.exit(failed > 0 ? 1 : 0);
    }

    /**
     * Test complete successful webhook flow from request to confirmation
     */
    public boolean testSuccessfulWebhookFlow() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"transaction_id\":\"tx_success_001\",\"amount\":\"99.99\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_success_001");

        boolean statusOk = (Integer) response.get("status") == 200;
        boolean messageOk = "Payment confirmation recorded".equals(response.get("message"));
        boolean transactionIdOk = "tx_success_001".equals(response.get("transactionId"));
        boolean requestIdOk = "req_success_001".equals(response.get("requestId"));

        PaymentWebhookHandler.PaymentConfirmation confirmation = handler.getConfirmation("tx_success_001");
        boolean confirmationRecorded = confirmation != null
            && confirmation.amount == 99.99
            && "USD".equals(confirmation.currency);

        controller.shutdown();

        return statusOk && messageOk && transactionIdOk && requestIdOk && confirmationRecorded;
    }

    /**
     * Test that invalid payment data (missing amount, invalid currency) is rejected
     */
    public boolean testInvalidPaymentDataRejected() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        // Missing amount
        String payload1 = "{\"transaction_id\":\"tx_invalid_001\",\"currency\":\"USD\"}";
        String signature1 = generateValidSignature(payload1);
        Map<String, Object> response1 = controller.processWebhook(payload1, signature1, "req_invalid_001");

        // Missing currency
        String payload2 = "{\"transaction_id\":\"tx_invalid_002\",\"amount\":\"50.00\"}";
        String signature2 = generateValidSignature(payload2);
        Map<String, Object> response2 = controller.processWebhook(payload2, signature2, "req_invalid_002");

        // Zero amount
        String payload3 = "{\"transaction_id\":\"tx_invalid_003\",\"amount\":\"0\",\"currency\":\"USD\"}";
        String signature3 = generateValidSignature(payload3);
        Map<String, Object> response3 = controller.processWebhook(payload3, signature3, "req_invalid_003");

        controller.shutdown();

        return (Integer) response1.get("status") == 400
            && (Integer) response2.get("status") == 400
            && (Integer) response3.get("status") == 400
            && handler.getConfirmationCount() == 0;
    }

    /**
     * Test that duplicate transaction IDs can be processed (overwrites previous)
     */
    public boolean testDuplicateTransactions() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload1 = "{\"transaction_id\":\"tx_dup_001\",\"amount\":\"100.00\",\"currency\":\"USD\"}";
        String signature1 = generateValidSignature(payload1);
        controller.processWebhook(payload1, signature1, "req_dup_001");

        String payload2 = "{\"transaction_id\":\"tx_dup_001\",\"amount\":\"150.00\",\"currency\":\"EUR\"}";
        String signature2 = generateValidSignature(payload2);
        controller.processWebhook(payload2, signature2, "req_dup_002");

        PaymentWebhookHandler.PaymentConfirmation confirmation = handler.getConfirmation("tx_dup_001");

        controller.shutdown();

        // Should have the second transaction's data
        return confirmation != null
            && confirmation.amount == 150.00
            && "EUR".equals(confirmation.currency)
            && handler.getConfirmationCount() == 1;
    }

    /**
     * Test multiple different successful webhooks
     */
    public boolean testMultipleWebhooks() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String[] payloads = {
            "{\"transaction_id\":\"tx_multi_001\",\"amount\":\"10.00\",\"currency\":\"USD\"}",
            "{\"transaction_id\":\"tx_multi_002\",\"amount\":\"20.00\",\"currency\":\"EUR\"}",
            "{\"transaction_id\":\"tx_multi_003\",\"amount\":\"30.00\",\"currency\":\"GBP\"}",
            "{\"transaction_id\":\"tx_multi_004\",\"amount\":\"40.00\",\"currency\":\"JPY\"}",
            "{\"transaction_id\":\"tx_multi_005\",\"amount\":\"50.00\",\"currency\":\"CAD\"}"
        };

        for (int i = 0; i < payloads.length; i++) {
            String signature = generateValidSignature(payloads[i]);
            controller.processWebhook(payloads[i], signature, "req_multi_" + (i + 1));
        }

        controller.shutdown();

        return handler.getConfirmationCount() == 5
            && handler.getConfirmation("tx_multi_001") != null
            && handler.getConfirmation("tx_multi_005") != null;
    }

    /**
     * Test webhook with missing transaction ID
     */
    public boolean testMissingFields() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"amount\":\"100.00\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_missing_001");

        controller.shutdown();

        return (Integer) response.get("status") == 400
            && handler.getConfirmationCount() == 0;
    }

    /**
     * Test various currencies are supported
     */
    public boolean testMultipleCurrencies() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF"};

        for (int i = 0; i < currencies.length; i++) {
            String payload = String.format(
                "{\"transaction_id\":\"tx_curr_%d\",\"amount\":\"100.00\",\"currency\":\"%s\"}",
                i, currencies[i]
            );
            String signature = generateValidSignature(payload);
            Map<String, Object> response = controller.processWebhook(payload, signature, "req_curr_" + i);

            if ((Integer) response.get("status") != 200) {
                controller.shutdown();
                return false;
            }
        }

        controller.shutdown();

        return handler.getConfirmationCount() == currencies.length;
    }

    /**
     * Test large transaction amounts
     */
    public boolean testLargeAmounts() {
        PaymentWebhookHandler handler = new PaymentWebhookHandler(0);
        WebhookController controller = new WebhookController(5000, handler);

        String payload = "{\"transaction_id\":\"tx_large_001\",\"amount\":\"9999999.99\",\"currency\":\"USD\"}";
        String signature = generateValidSignature(payload);

        Map<String, Object> response = controller.processWebhook(payload, signature, "req_large_001");

        PaymentWebhookHandler.PaymentConfirmation confirmation = handler.getConfirmation("tx_large_001");

        controller.shutdown();

        return (Integer) response.get("status") == 200
            && confirmation != null
            && confirmation.amount == 9999999.99;
    }

    /**
     * Generate a valid signature for testing
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
