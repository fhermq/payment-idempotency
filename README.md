# Payment Idempotency Demo

A production-grade reference implementation demonstrating best practices for implementing idempotency patterns in Java/Spring Boot applications.

## 🎯 Project Overview

This project showcases how to build a robust payment processing system with idempotency guarantees. It serves as an educational reference for developers looking to understand and implement idempotent APIs in Spring Boot.

**Key Technologies:**
- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- H2 Database
- Lombok

**Status:** ✅ Reference Implementation (Educational)

## 🌟 Features

### Core Idempotency Pattern
- **Idempotency Key Management**: Ensures duplicate requests are safely handled
- **State Management**: Tracks payment processing states (PENDING → PROCESSING → COMPLETED/FAILED)
- **Conflict Detection**: Returns cached responses for duplicate requests
- **Transactional Safety**: ACID guarantees for payment operations

### Production-Grade Components
- **Global Exception Handling**: Centralized error management with error codes
- **Standardized Error Responses**: Structured error DTOs with correlation IDs
- **Input Validation**: Jakarta Bean Validation with meaningful error messages
- **Logging with Traceability**: Correlation IDs for request tracking
- **Proper HTTP Semantics**: Correct status codes and response structures

### Database Design
- **H2 In-Memory Database**: Perfect for development and testing
- **JPA Entities**: Well-structured domain models with constraints
- **Idempotency Record Table**: Tracks requests for duplicate detection
- **Payment Entity**: Comprehensive payment information storage

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/fhermq/payment-idempotency.git
   cd payment-idempotency
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will start on `http://localhost:8080`

### Verify Installation
```bash
# Access H2 Console (optional)
http://localhost:8080/h2-console

# Run tests
mvn test
```

## 📚 API Documentation

### Process Payment (Idempotent)

**Endpoint:** `POST /api/payments`

**Request:**
```json
{
  "idempotencyKey": "unique-request-id-12345",
  "amount": 100.50,
  "description": "Payment for order #123",
  "customerEmail": "customer@example.com"
}
```

**Response (Success - 201 Created):**
```json
{
  "paymentId": "PAY-001",
  "amount": 100.50,
  "status": "COMPLETED",
  "description": "Payment for order #123",
  "timestamp": "2025-01-19T10:30:00Z",
  "idempotencyKey": "unique-request-id-12345"
}
```

**Response (Duplicate Request - 409 Conflict):**
```json
{
  "paymentId": "PAY-001",
  "amount": 100.50,
  "status": "COMPLETED",
  "description": "Payment for order #123",
  "timestamp": "2025-01-19T10:30:00Z",
  "idempotencyKey": "unique-request-id-12345"
}
```

**Error Response:**
```json
{
  "errorCode": "INVALID_AMOUNT",
  "message": "Amount must be greater than 0",
  "correlationId": "corr-123-456-789",
  "timestamp": "2025-01-19T10:30:00Z"
}
```

### Get Payment Status

**Endpoint:** `GET /api/payments/{paymentId}`

**Response:**
```json
{
  "paymentId": "PAY-001",
  "amount": 100.50,
  "status": "COMPLETED",
  "description": "Payment for order #123",
  "timestamp": "2025-01-19T10:30:00Z",
  "idempotencyKey": "unique-request-id-12345"
}
```

## 🏗️ Project Architecture

```
src/
├── main/java/com/fhermq/payment/
│   ├── PaymentIdempotencyApplication.java    # Spring Boot Application
│   ├── controller/                           # REST Endpoints
│   │   └── PaymentController.java
│   ├── service/                              # Business Logic
│   │   └── PaymentService.java
│   ├── repository/                           # Data Access
│   │   ├── PaymentRepository.java
│   │   └── IdempotencyRecordRepository.java
│   ├── entity/                               # JPA Entities
│   │   ├── Payment.java
│   │   └── IdempotencyRecord.java
│   ├── dto/                                  # Data Transfer Objects
│   │   ├── PaymentRequest.java
│   │   ├── PaymentResponse.java
│   │   └── ErrorResponse.java
│   ├── enums/                                # Enumerations
│   │   ├── PaymentStatus.java
│   │   └── RequestStatus.java
│   └── exception/                            # Exception Handling
│       ├── ApiException.java
│       ├── PaymentException.java
│       ├── IdempotencyException.java
│       ├── GlobalExceptionHandler.java
│       └── ErrorCode.java
├── resources/
│   ├── application.yaml                      # Configuration
│   ├── static/                               # Static Files
│   └── templates/                            # Templates
└── test/java/com/fhermq/payment/
    └── PaymentIdempotencyTest.java           # Test Cases
```

## 🔑 Key Concepts

### Idempotency Pattern

An idempotent operation produces the same result regardless of how many times it's executed. This is critical for:
- **Network Retries**: Clients can safely retry failed requests
- **Duplicate Prevention**: Multiple identical requests are treated as one
- **Financial Accuracy**: Prevents duplicate charges

### Implementation Strategy

1. **Accept Idempotency Key**: Client provides a unique identifier for each logical request
2. **Check Existing Record**: Query the idempotency table before processing
3. **Return Cached Result**: If found, return the previous response
4. **Process & Store**: If new, process the payment and store the result
5. **Atomic Operations**: Use transactions to ensure data consistency

### State Transitions

```
PENDING → PROCESSING → COMPLETED
              ↓
            FAILED
```

## 💡 Best Practices Demonstrated

### 1. **Error Handling**
- Structured error responses with error codes
- Environment-aware error details (security-first)
- Correlation IDs for request tracing
- Custom exception hierarchy

### 2. **Data Validation**
- Input validation using Jakarta Bean Validation
- Business logic validation in service layer
- Meaningful error messages for clients

### 3. **Transaction Management**
- `@Transactional` annotations for ACID guarantees
- Proper handling of transaction boundaries
- Conflict detection for concurrent requests

### 4. **REST API Design**
- Proper HTTP status codes (201, 409, 400, 500)
- Idempotent POST operations
- Meaningful resource identifiers
- Standard error response format

### 5. **Code Organization**
- Clear separation of concerns (layered architecture)
- DTOs for request/response contracts
- Entities for persistence
- Services for business logic

## 🧪 Testing

Run the comprehensive test suite:

```bash
mvn test
```

Tests cover:
- ✅ Successful payment processing
- ✅ Duplicate request handling
- ✅ Invalid amount validation
- ✅ Error response format
- ✅ Payment status retrieval

## ⚙️ Configuration

The application is configured via `application.yaml`:

```yaml
spring:
  application:
    name: payment-idempotency
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.fhermq.payment: DEBUG
```

### Environment Variables

You can override properties using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:h2:file:./payment-db
export LOGGING_LEVEL_COM_FHERMQ_PAYMENT=INFO
mvn spring-boot:run
```

## 📊 Database Schema

### Payment Table
```sql
CREATE TABLE payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_id VARCHAR(50) UNIQUE NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  status VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  customer_email VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payment_id ON payment(payment_id);
CREATE INDEX idx_status ON payment(status);
```

### Idempotency Record Table
```sql
CREATE TABLE idempotency_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  idempotency_key VARCHAR(255) UNIQUE NOT NULL,
  request_status VARCHAR(50) NOT NULL,
  payment_id VARCHAR(50),
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_key ON idempotency_record(idempotency_key);
```

## 🔒 Security Considerations

### Production Deployment

**⚠️ Important:** This is a reference implementation. For production deployment, consider:

- **Authentication & Authorization**: Implement OAuth2/JWT
- **Rate Limiting**: Prevent abuse with request throttling
- **Encryption**: Use TLS for data in transit
- **Database Security**: Use encrypted credentials
- **Audit Logging**: Track all financial transactions
- **PCI Compliance**: If handling real cards, ensure compliance
- **Monitoring**: Set up alerts for anomalies

### Error Information

Error responses include different details based on environment:
- **Development**: Full stack traces and SQL queries
- **Production**: Generic messages with correlation IDs for support

## 🤝 Contributing

This is an educational reference project. Feel free to:
- Study the implementation
- Adapt patterns for your projects
- Report issues or suggest improvements
- Create learning resources based on this code

## 📖 Learning Resources

### External Resources
- [Spring Boot Official Guide](https://spring.io/projects/spring-boot)
- [REST API Best Practices](https://restfulapi.net/)
- [Idempotency Patterns](https://stripe.com/blog/idempotency)
- [Database Transactions](https://www.postgresql.org/docs/current/tutorial-transactions.html)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)

## 📝 License

This project is provided as-is for educational purposes. Feel free to use it as a reference for your own projects.


---
