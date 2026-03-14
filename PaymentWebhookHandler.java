import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PaymentWebhookHandler processes payment confirmation webhooks
 * and records transaction confirmations.
 */
public class PaymentWebhookHandler {
    // In-memory store for demo purposes. In production, use a database.
    private final Map<String, PaymentConfirmation> confirmations;
    private final long processingDelayMs;

    public PaymentWebhookHandler() {
        this(0); // No artificial delay by default
    }

    public PaymentWebhookHandler(long processingDelayMs) {
        this.confirmations = new ConcurrentHashMap<>();
        this.processingDelayMs = processingDelayMs;
    }

    /**
     * Process a payment webhook payload
     * @param payload The JSON payload from the payment gateway
     * @param requestId The request ID for tracking
     * @return Response map with processing status
     */
    public Map<String, Object> process(String payload, String requestId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Simulate processing delay if configured (for testing timeout scenarios)
            if (processingDelayMs > 0) {
                Thread.sleep(processingDelayMs);
            }

            // Parse payment data from payload
            PaymentData paymentData = parsePayload(payload);

            // Validate payment data
            if (!isValidPaymentData(paymentData)) {
                response.put("status", 400);
                response.put("error", "Invalid payment data");
                response.put("requestId", requestId);
                return response;
            }

            // Record the confirmation
            PaymentConfirmation confirmation = new PaymentConfirmation(
                paymentData.transactionId,
                paymentData.amount,
                paymentData.currency,
                Instant.now(),
                requestId
            );

            confirmations.put(paymentData.transactionId, confirmation);

            // Return success response
            response.put("status", 200);
            response.put("message", "Payment confirmation recorded");
            response.put("transactionId", paymentData.transactionId);
            response.put("requestId", requestId);
            response.put("recordedAt", confirmation.recordedAt.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.put("status", 500);
            response.put("error", "Processing interrupted");
            response.put("requestId", requestId);

        } catch (Exception e) {
            response.put("status", 500);
            response.put("error", "Processing failed: " + e.getMessage());
            response.put("requestId", requestId);
        }

        return response;
    }

    /**
     * Parse payment data from JSON payload
     * Simplified parsing for demo - in production use a proper JSON library
     */
    private PaymentData parsePayload(String payload) {
        PaymentData data = new PaymentData();

        // Simple key-value extraction (in production, use Jackson/Gson)
        if (payload.contains("transaction_id")) {
            data.transactionId = extractValue(payload, "transaction_id");
        }
        if (payload.contains("amount")) {
            String amountStr = extractValue(payload, "amount");
            try {
                data.amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                data.amount = 0.0;
            }
        }
        if (payload.contains("currency")) {
            data.currency = extractValue(payload, "currency");
        }

        return data;
    }

    /**
     * Simple value extraction from JSON-like string
     */
    private String extractValue(String payload, String key) {
        int keyIndex = payload.indexOf("\"" + key + "\"");
        if (keyIndex == -1) {
            return "";
        }

        int colonIndex = payload.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return "";
        }

        int startQuote = payload.indexOf("\"", colonIndex);
        if (startQuote == -1) {
            // Try to find number value
            int endComma = payload.indexOf(",", colonIndex);
            int endBrace = payload.indexOf("}", colonIndex);
            int endIndex = endComma != -1 ? endComma : endBrace;
            if (endIndex != -1) {
                return payload.substring(colonIndex + 1, endIndex).trim();
            }
            return "";
        }

        int endQuote = payload.indexOf("\"", startQuote + 1);
        if (endQuote == -1) {
            return "";
        }

        return payload.substring(startQuote + 1, endQuote);
    }

    /**
     * Validate payment data
     */
    private boolean isValidPaymentData(PaymentData data) {
        return data.transactionId != null && !data.transactionId.isEmpty()
            && data.amount > 0
            && data.currency != null && !data.currency.isEmpty();
    }

    /**
     * Get a payment confirmation by transaction ID
     */
    public PaymentConfirmation getConfirmation(String transactionId) {
        return confirmations.get(transactionId);
    }

    /**
     * Get total number of recorded confirmations
     */
    public int getConfirmationCount() {
        return confirmations.size();
    }

    /**
     * Clear all confirmations (for testing)
     */
    public void clearConfirmations() {
        confirmations.clear();
    }

    /**
     * Payment data extracted from webhook payload
     */
    private static class PaymentData {
        String transactionId;
        double amount;
        String currency;
    }

    /**
     * Payment confirmation record
     */
    public static class PaymentConfirmation {
        public final String transactionId;
        public final double amount;
        public final String currency;
        public final Instant recordedAt;
        public final String requestId;

        public PaymentConfirmation(String transactionId, double amount,
                                  String currency, Instant recordedAt, String requestId) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.currency = currency;
            this.recordedAt = recordedAt;
            this.requestId = requestId;
        }
    }
}
