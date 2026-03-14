# Payment Gateway Webhook Handler

This implementation provides a robust webhook handling system with comprehensive timeout protection and signature validation.

## Features

### 1. **Timeout Handling**
- Configurable request timeout (default: 5 seconds, max: 30 seconds)
- Automatic timeout detection and cleanup
- Graceful error responses with timeout indicators
- Thread-safe concurrent request processing

### 2. **Signature Validation**
- HMAC-SHA256 signature verification
- Constant-time comparison to prevent timing attacks
- Configurable secret key via environment variables
- Protection against replay and tampering attacks

### 3. **Payment Processing**
- Transaction confirmation recording
- Support for multiple currencies
- Validation of payment data
- Request tracking with unique IDs

## Components

### WebhookController
Main controller that handles incoming webhook requests with:
- Timeout protection using ExecutorService
- Signature validation
- Request/response lifecycle management

**Key Methods:**
- `processWebhook(payload, signature, requestId)` - Main entry point
- `validateSignature(payload, signature)` - HMAC-SHA256 validation

### PaymentWebhookHandler
Processes payment confirmations:
- Parses webhook payloads
- Validates payment data
- Records confirmations
- Supports configurable processing delays (for testing)

### Test Suites

#### WebhookTimeoutTest
Tests timeout handling scenarios:
- ✓ Fast processing completes successfully
- ✓ Slow processing triggers timeout
- ✓ Timeout responses include proper error details
- ✓ Concurrent requests handle timeouts independently
- ✓ Custom timeout values are respected
- ✓ Max timeout cap is enforced
- ✓ Valid signatures process successfully
- ✓ Invalid signatures are rejected

#### WebhookIntegrationTest
Tests end-to-end webhook processing:
- ✓ Complete successful webhook flow
- ✓ Invalid payment data rejection
- ✓ Duplicate transaction handling
- ✓ Multiple webhook recording
- ✓ Missing field validation
- ✓ Multiple currency support
- ✓ Large transaction amounts

## Configuration

### Environment Variables

```bash
# Webhook secret key for signature validation
export WEBHOOK_SECRET="your-secret-key-here"
```

**IMPORTANT:** Change the default secret in production!

### Timeout Configuration

```java
// Default 5-second timeout
WebhookController controller = new WebhookController();

// Custom 10-second timeout
WebhookController controller = new WebhookController(10000);

// Max timeout is capped at 30 seconds
WebhookController controller = new WebhookController(60000); // Will use 30000ms
```

## Usage Example

```java
// Create controller with custom timeout
PaymentWebhookHandler handler = new PaymentWebhookHandler();
WebhookController controller = new WebhookController(5000, handler);

// Process webhook request
String payload = "{\"transaction_id\":\"tx_123\",\"amount\":\"99.99\",\"currency\":\"USD\"}";
String signature = "sha256=abc123..."; // From webhook header
String requestId = "req_001";

Map<String, Object> response = controller.processWebhook(payload, signature, requestId);

if ((Integer) response.get("status") == 200) {
    System.out.println("Payment confirmed: " + response.get("transactionId"));
} else if (response.containsKey("timeout")) {
    System.out.println("Request timeout: " + response.get("error"));
} else {
    System.out.println("Error: " + response.get("error"));
}

// Cleanup when done
controller.shutdown();
```

## Response Format

### Success Response (200)
```json
{
  "status": 200,
  "message": "Payment confirmation recorded",
  "transactionId": "tx_123",
  "requestId": "req_001",
  "recordedAt": "2024-01-15T14:23:41Z"
}
```

### Timeout Response (408)
```json
{
  "status": 408,
  "error": "Webhook processing timeout after 5000ms",
  "requestId": "req_001",
  "timeout": true
}
```

### Invalid Signature (401)
```json
{
  "status": 401,
  "error": "Invalid signature verification",
  "requestId": "req_001"
}
```

### Invalid Data (400)
```json
{
  "status": 400,
  "error": "Invalid payment data",
  "requestId": "req_001"
}
```

## Running Tests

```bash
# Compile all files
javac WebhookController.java PaymentWebhookHandler.java \
      WebhookTimeoutTest.java WebhookIntegrationTest.java

# Run timeout tests
java WebhookTimeoutTest

# Run integration tests
java WebhookIntegrationTest
```

## Security Considerations

1. **Always use HTTPS** for webhook endpoints in production
2. **Set a strong webhook secret** via environment variable
3. **Validate all incoming data** before processing
4. **Log failed validation attempts** for security monitoring
5. **Rate limit** webhook endpoints to prevent abuse
6. **Monitor timeout rates** to detect potential attacks or issues

## Addressing the Original Issue

The error log showed:
```
ERROR: Webhook validation failed
PaymentGatewayException: Invalid signature verification
Status: 401 Unauthorized
Signature: sha256=mismatch
```

This implementation addresses the issue by:

1. **Proper signature validation** using HMAC-SHA256
2. **Timeout handling** to prevent hanging requests (15% failure rate)
3. **Comprehensive error responses** with request tracking
4. **Thread-safe concurrent processing** to handle multiple webhooks
5. **Extensive test coverage** to prevent regressions

## Monitoring & Debugging

To debug signature mismatches:
1. Verify `WEBHOOK_SECRET` environment variable matches gateway config
2. Check payload encoding (UTF-8)
3. Ensure no whitespace modifications to payload
4. Verify signature header format: `sha256=<hex>`

To debug timeouts:
1. Monitor processing time per request
2. Check for blocking operations
3. Review timeout configuration (default: 5s)
4. Analyze concurrent load patterns

## Production Deployment

Before deploying:
- [ ] Set `WEBHOOK_SECRET` environment variable
- [ ] Configure appropriate timeout values
- [ ] Set up monitoring and alerting
- [ ] Implement database persistence (replace in-memory storage)
- [ ] Add proper logging
- [ ] Configure rate limiting
- [ ] Set up TLS/SSL certificates
- [ ] Test with payment gateway sandbox
