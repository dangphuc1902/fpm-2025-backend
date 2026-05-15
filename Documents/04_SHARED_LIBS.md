# 04 — Shared Libraries (`fpm-libs`)

> **Version:** 1.0 | **Updated:** 2026-05-15  
> **Location:** `libs/fpm-libs/`

---

## Tổng quan

`fpm-libs` là monorepo chứa **9 module thư viện nội bộ** dùng chung cho tất cả microservices. Thay vì copy-paste logic giữa các service, các module này được build và publish như Maven artifacts nội bộ.

> 💡 **BOM (Bill of Materials)** — Một POM đặc biệt chỉ chứa `<dependencyManagement>`, không có code. Các service khác import BOM để thừa hưởng toàn bộ version đã được quy định mà không cần khai báo `<version>` cho từng dependency.

**Dependency hierarchy:**

```
fpm-bom
    └── fpm-domain          (shared models/events/DTOs)
    └── fpm-core            (base infra: JWT, Redis, Exception)
    └── fpm-common          (cross-cutting: logging, CORS, Jackson)
    └── fpm-security        (JWT provider, auth filter)
    └── fpm-grpc            (gRPC interceptors, client config)
    └── fpm-proto           (Protobuf generated stubs)
    └── fpm-messaging       (Kafka + RabbitMQ config)
    └── fpm-testing         (Test utilities, Testcontainers)
```

---

## 1. `fpm-bom` — Bill of Materials

### Mục đích

Quản lý version tập trung cho tất cả dependencies. Service chỉ cần import BOM, không cần khai báo `<version>` riêng.

### Cách sử dụng

```xml
<!-- Trong pom.xml của mỗi service -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fpm2025</groupId>
            <artifactId>fpm-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Versions được quản lý

| Dependency | Version |
|-----------|---------|
| Spring Boot | 3.5.5 |
| Spring Cloud | 2025.0.0 |
| Spring gRPC | 0.10.0 |
| Protobuf | 4.30.2 |
| JJWT | 0.12.5 |
| Kafka | Confluent 7.5.0 |
| Resilience4j | BOM managed |
| MySQL Connector | BOM managed |

---

## 2. `fpm-domain` — Shared Domain Model

### Mục đích

**Shared Kernel** — chứa các model được chia sẻ giữa các Bounded Contexts. Không chứa business logic, chỉ chứa data structures.

> ⚠️ **Rule:** Chỉ thêm class vào `fpm-domain` khi ≥ 2 services cùng dùng. Nếu chỉ 1 service dùng → đặt trong service đó.

### Package Structure

```
com.fpm2025.domain/
├── common/
│   ├── BaseResponse<T>           ← Standard API response wrapper
│   └── Money.java                ← Value Object: amount + currency
├── constants/
│   ├── DomainConstants.java      ← Nested: Wallet, Cache, Event, Transaction
│   └── ErrorMessages.java        ← Standard error message strings
├── dto/
│   ├── request/
│   │   ├── TransactionRequest.java
│   │   ├── UpdateTransactionRequest.java
│   │   ├── ShareWalletRequest.java
│   │   └── BankNotificationRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── WalletResponse.java
│       ├── WalletPermissionResponse.java
│       ├── TransactionResponse.java
│       ├── CategoryResponse.java
│       ├── FamilyResponse.java
│       └── PageResponse<T>.java
├── event/
│   ├── UserCreatedEvent.java         ← Kafka: user.created
│   ├── TransactionCreatedEvent.java  ← Kafka: transaction.created
│   ├── WalletCreatedEvent.java       ← Kafka: wallet.created
│   ├── BalanceUpdateEvent.java       ← Kafka: balance.changed
│   └── ParsedNotificationEvent.java  ← Kafka: notification.parsed
├── enums/
│   ├── CategoryType     → INCOME, EXPENSE
│   ├── WalletType       → CASH, CARD, BANK, INVESTMENT
│   └── CurrencyCode     → VND, USD, EUR, ...
└── transaction/
    ├── Transaction.java              ← Domain model (DDD aggregate)
    └── TransactionCreatedEvent.java  ← Domain event
```

### `BaseResponse<T>` — Standard API Response

```java
// Mọi REST API đều trả về format này
public class BaseResponse<T> {
    private int code;        // HTTP status code (200, 201, 400...)
    private String message;  // Human-readable message
    private T data;          // Actual payload

    public static <T> BaseResponse<T> success(T data, String message) { ... }
    public static <T> BaseResponse<T> error(T data, String message) { ... }
}
```

**Ví dụ response thực tế:**
```json
{
  "code": 200,
  "message": "Wallet created successfully",
  "data": {
    "id": 1,
    "name": "Ví tiền mặt",
    "balance": 500000
  }
}
```

### `DomainConstants` — Constants tập trung

```java
public class DomainConstants {
    public static class Wallet {
        public static final BigDecimal MIN_BALANCE = BigDecimal.ZERO;
        public static final BigDecimal MAX_BALANCE = new BigDecimal("1000000000");
        public static final int MAX_WALLETS_PER_USER = 10;
    }
    public static class Cache {
        public static final long REPORT_TTL_SECONDS = 300;    // 5 phút
        public static final long DASHBOARD_TTL_SECONDS = 300; // 5 phút
        public static final long TOKEN_TTL_SECONDS = 86400;   // 24h
    }
    public static class Event {
        public static final String TRANSACTION_CREATED = "transaction.created";
        public static final String USER_CREATED = "user.created";
        public static final String WALLET_CREATED = "wallet.created";
        // ...
    }
}
```

---

## 3. `fpm-core` — Core Infrastructure

### Mục đích

Cung cấp các infrastructure components tái sử dụng: exception handling, response mapping, Redis config, Swagger config.

### Package Structure & Classes

```
com.fpm2025.core/
├── dto/
│   ├── response/
│   │   ├── BaseResponse<T>       ← Duplicate từ fpm-domain (legacy)
│   │   ├── ErrorResponse.java    ← Chi tiết lỗi validation
│   │   └── PageResponse<T>.java  ← Pagination wrapper
│   └── mapper/
│       ├── MapperRegistry.java   ← Registry pattern cho object mappers
│       └── MapperFactory.java    ← Factory tạo mapper instance
├── exception/
│   ├── GlobalExceptionHandler.java    ← @ControllerAdvice xử lý mọi exception
│   ├── ResourceNotFoundException.java ← 404
│   ├── UnauthorizedException.java     ← 401
│   ├── ForbiddenException.java        ← 403
│   ├── DuplicateResourceException.java← 409
│   └── ValidationException.java       ← 400 validation
├── security/
│   ├── JwtService.java               ← JWT operations (fpm-core version)
│   └── JwtAuthenticationFilter.java  ← Filter chain (fpm-core version)
├── config/
│   ├── RedisConfig.java         ← RedisTemplate + StringRedisTemplate beans
│   └── SwaggerConfig.java       ← OpenAPI 3 config
├── properties/
│   └── FpmCoreProperties.java   ← @ConfigurationProperties("fpm.core")
└── util/
    ├── DateTimeUtil.java         ← Format, parse, timezone conversion
    ├── PageableUtil.java         ← Build Pageable từ page/size/sort params
    └── ValidationUtil.java       ← Email format, phone format checks
```

### `GlobalExceptionHandler` — Centralized Error Handling

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    // → HTTP 404 + BaseResponse.error(message)

    @ExceptionHandler(UnauthorizedException.class)
    // → HTTP 401

    @ExceptionHandler(ForbiddenException.class)
    // → HTTP 403

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // → HTTP 400 + list of field errors (từ Bean Validation)

    @ExceptionHandler(DuplicateResourceException.class)
    // → HTTP 409

    @ExceptionHandler(Exception.class)
    // → HTTP 500 + generic error message (không lộ stack trace)
}
```

> 💡 **Tại sao dùng `@ControllerAdvice`?** Thay vì mỗi controller try-catch từng exception, một class global xử lý tất cả. Services chỉ cần `throw new ResourceNotFoundException("Wallet not found")` — framework tự trả về 404 đúng format.

---

## 4. `fpm-common` — Cross-Cutting Concerns

### Mục đích

Các concerns không thuộc business nhưng cần có ở mọi service: logging, CORS, Jackson serialization, JPA auditing.

### Classes

```
com.fpm_2025.fpm_microservice_libs/
├── filter/
│   ├── RequestLoggingFilter.java  ← Log mọi request: method, path, duration
│   └── CorrelationIdFilter.java   ← Inject X-Correlation-Id header vào mọi request
├── config/
│   ├── web/WebMvcConfig.java      ← CORS config (allow origins, methods)
│   ├── jackson/JacksonConfig.java ← Custom serializers (LocalDateTime → ISO string)
│   └── auditing/
│       ├── JpaAuditingConfig.java    ← Enable @EnableJpaAuditing
│       └── AuditorAwareImpl.java     ← Lấy userId từ SecurityContext cho @CreatedBy
└── Aspects/
    ├── LoggingAspect.java         ← AOP: log method entry/exit cho @Service
    └── PerformanceAspect.java     ← AOP: log method execution time > threshold
```

### `CorrelationIdFilter` — Request Tracing

```java
// Mỗi request được gán một ID duy nhất để trace qua nhiều services
public class CorrelationIdFilter implements Filter {
    public void doFilter(request, response, chain) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);  // Xuất hiện trong mọi log line
        response.setHeader("X-Correlation-Id", correlationId);
        chain.doFilter(request, response);
    }
}
```

### `PerformanceAspect` — AOP Performance Monitoring

```java
// Tự động log method nào chạy lâu hơn threshold
@Around("@within(org.springframework.stereotype.Service)")
public Object measureTime(ProceedingJoinPoint joinPoint) {
    long start = System.currentTimeMillis();
    Object result = joinPoint.proceed();
    long duration = System.currentTimeMillis() - start;
    if (duration > 500) { // ms
        log.warn("SLOW METHOD: {}.{}() took {}ms",
            className, methodName, duration);
    }
    return result;
}
```

---

## 5. `fpm-security` — Security Components

### Mục đích

JWT generation, validation, và Spring Security filter — tái sử dụng ở cả API Gateway và mỗi service.

### Classes

```
com.fpm2025.security/
├── jwt/
│   ├── JwtTokenProvider.java         ← Core JWT operations
│   ├── JwtAuthenticationFilter.java  ← OncePerRequestFilter
│   └── JwtProperties.java            ← @ConfigurationProperties("jwt")
├── user/
│   └── UserPrincipal.java            ← UserDetails implementation
├── grpc/
│   ├── GrpcAuthInterceptor.java      ← ServerInterceptor xác thực gRPC calls
│   └── GrpcSecurityContext.java      ← Thread-local security context cho gRPC
└── util/
    └── PasswordEncoderUtil.java       ← BCryptPasswordEncoder singleton
```

### `JwtTokenProvider` — JWT Core

```java
public class JwtTokenProvider {

    // Generate token với claims: userId, email, role
    public String generateToken(Long userId, String email, String role)

    // Verify signature + expiration
    public boolean validateToken(String token)

    // Extract claims từ token (không verify — dùng sau validateToken)
    public Long extractUserId(String token)
    public String extractEmail(String token)
    public String extractRole(String token)
    public Date extractExpiration(String token)
}
```

**Config** (từ `JwtProperties`):
```yaml
jwt:
  secret: ${JWT_SECRET}     # HS256 signing key
  expiration: 86400000      # 24h (milliseconds)
```

### `JwtAuthenticationFilter` — Request Filter

```java
// Chạy một lần duy nhất per request (OncePerRequestFilter)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(request, response, chain) {
        // 1. Extract "Bearer {token}" từ Authorization header
        // 2. jwtTokenProvider.validateToken(token)
        // 3. Tạo UsernamePasswordAuthenticationToken
        // 4. Set vào SecurityContextHolder
        // 5. chain.doFilter() → tiếp tục xử lý request
    }
}
```

### `GrpcAuthInterceptor` — gRPC Security

```java
// Intercept mọi gRPC call, extract userId từ metadata
public class GrpcAuthInterceptor implements ServerInterceptor {
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        String token = headers.get(AUTHORIZATION_KEY);
        // Validate → set GrpcSecurityContext
        return next.startCall(call, headers);
    }
}
```

---

## 6. `fpm-grpc` — gRPC Infrastructure

### Mục đích

gRPC interceptors, client channel configuration, shared gRPC utilities.

### Classes

```
com.fpm2025.grpc/
├── interceptor/
│   ├── GrpcLoggingInterceptor.java  ← Log mọi gRPC call: method, duration, status
│   └── GrpcErrorInterceptor.java    ← Convert Java exceptions → gRPC Status codes
└── config/
    └── GrpcClientConfig.java        ← ManagedChannel factory với Eureka discovery
```

### `GrpcErrorInterceptor` — Error Mapping

```java
// Tự động convert Java exceptions → gRPC status
public class GrpcErrorInterceptor implements ServerInterceptor {
    // ResourceNotFoundException → Status.NOT_FOUND
    // UnauthorizedException    → Status.UNAUTHENTICATED
    // ForbiddenException       → Status.PERMISSION_DENIED
    // Exception                → Status.INTERNAL
}
```

### `GrpcClientConfig` — Channel Setup

```java
@Configuration
public class GrpcClientConfig {
    // Tạo ManagedChannel với Eureka name resolution
    public ManagedChannel createChannel(String serviceName) {
        return ManagedChannelBuilder
            .forTarget("discovery:///" + serviceName)
            .usePlaintext()  // TLS disabled (dev mode)
            .build();
    }
}
```

---

## 7. `fpm-proto` — Protobuf Contracts

### Mục đích

Single source of truth cho tất cả gRPC API contracts. Auto-generate Java classes từ `.proto` files khi build.

### Proto Files

| File | gRPC Service | Key Messages |
|------|-------------|-------------|
| `common.proto` | — | `Money`, `PageResponse`, `UserIdRequest`, `FamilyIdRequest` |
| `user.proto` | `UserGrpcService` | `TokenRequest`, `UserResponse`, `FamilyMembersResponse` |
| `wallet.proto` | `WalletGrpcService` | `WalletResponse`, `UpdateBalanceRequest`, `BalanceCheckRequest` |
| `transaction.proto` | `TransactionGrpcService` | `TransactionResponse`, `DateRangeRequest`, `SpendingResponse` |
| `category.proto` | `CategoryGrpcService` | Category CRUD messages |
| `sharing.proto` | `SharingGrpcService` | Sharing permission messages |

### Build Process

```xml
<!-- pom.xml của fpm-proto -->
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <!-- Proto → Java classes auto-generated vào target/generated-sources -->
</plugin>
```

**Generated classes location:** `target/generated-sources/protobuf/java/com/fpm2025/grpc/protocol/`

---

## 8. `fpm-messaging` — Messaging Infrastructure

### Mục đích

Kafka và RabbitMQ configuration, serialization, idempotency, DLQ handling.

### Package Structure

```
com.fpm2025.messaging/
├── kafka/
│   ├── config/
│   │   ├── KafkaProducerConfig.java   ← Producer beans + serializer setup
│   │   ├── KafkaConsumerConfig.java   ← Consumer beans + deserializer
│   │   └── KafkaTopicConfig.java      ← @Bean NewTopic for all 8 topics
│   ├── producer/
│   │   └── EventPublisher.java        ← Wrapper quanh KafkaTemplate
│   ├── consumer/
│   │   ├── BaseEventConsumer.java        ← Base class cho consumers
│   │   ├── EventIdempotencyChecker.java  ← Redis-based dedup (tránh xử lý 2 lần)
│   │   └── DeadLetterQueuePublisher.java ← Gửi failed event vào DLQ topic
│   └── serializer/
│       ├── JsonSerializer<T>.java     ← Object → JSON bytes
│       └── JsonDeserializer<T>.java   ← JSON bytes → Object
├── rabbitmq/
│   ├── config/
│   │   ├── RabbitMQConfig.java        ← Connection factory, message converter
│   │   └── RabbitMQEventConfig.java   ← Exchange + Queue + Binding declarations
│   └── producer/
│       └── TaskPublisher.java         ← RabbitTemplate wrapper
└── event/
    ├── model/
    │   └── EventMetadata.java         ← eventId, timestamp, source, version
    ├── publisher/
    │   └── RabbitEventPublisher.java  ← implements EventPublisher (RabbitMQ)
    └── listener/
        └── EventListenerRegistry.java ← Dynamic listener registration
```

### `EventIdempotencyChecker` — Chống xử lý trùng lặp

```java
// Dùng Redis để đảm bảo mỗi event chỉ xử lý 1 lần
public class EventIdempotencyChecker {
    private final RedisTemplate<String, String> redis;

    public boolean isProcessed(String eventId) {
        String key = "processed:event:" + eventId;
        Boolean exists = redis.hasKey(key);
        if (Boolean.TRUE.equals(exists)) return true;
        // Mark as processed với TTL 24h
        redis.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
        return false;
    }
}
```

> 💡 **Idempotency** — Đảm bảo xử lý event nhiều lần cho kết quả như xử lý 1 lần. Quan trọng khi Kafka retry hoặc consumer restart gây consume event 2 lần.

### `KafkaTopicConfig` — Topic Declarations

```java
@Configuration
public class KafkaTopicConfig {
    @Bean public NewTopic transactionCreated() { return new NewTopic("transaction.created", 3, (short) 1); }
    @Bean public NewTopic userCreated()        { return new NewTopic("user.created", 1, (short) 1); }
    @Bean public NewTopic walletCreated()      { return new NewTopic("wallet.created", 1, (short) 1); }
    @Bean public NewTopic notificationParsed() { return new NewTopic("notification.parsed", 2, (short) 1); }
    // ... 8 topics total
    // Parameters: (name, partitions, replicationFactor)
}
```

### RabbitMQ Exchange/Queue Declarations

```java
@Configuration
public class RabbitMQEventConfig {
    @Bean Exchange walletExchange() { return new DirectExchange("wallet.exchange"); }
    @Bean Queue walletCreatedQueue() { return new Queue("wallet.created.queue"); }
    @Bean Binding walletCreatedBinding() {
        return BindingBuilder.bind(walletCreatedQueue())
            .to(walletExchange()).with("wallet.created").noargs();
    }
}
```

---

## 9. `fpm-testing` — Testing Utilities

### Mục đích

Testcontainers base configs, test data factories, custom assertions — giúp viết integration test nhanh hơn.

### Classes (inferred từ documentation)

```
com.fpm2025.testing/
├── containers/
│   ├── MySQLContainerBase.java      ← Testcontainers MySQL 8.0
│   ├── RedisContainerBase.java      ← Testcontainers Redis 7
│   └── KafkaContainerBase.java      ← Testcontainers Kafka
├── factory/
│   └── TestDataFactory.java         ← Builder pattern tạo test entities
└── assertion/
    └── ApiResponseAssert.java       ← AssertJ custom assertions cho BaseResponse
```

### Cách dùng trong Integration Tests

```java
@SpringBootTest
class WalletServiceIntegrationTest extends MySQLContainerBase {
    // MySQL container tự động start trước test
    // Application kết nối vào container DB thay vì MySQL thật

    @Test void createWallet_success() {
        UserEntity user = TestDataFactory.createUser();
        // ... test logic
        ApiResponseAssert.assertThat(response)
            .hasStatusCode(201)
            .hasMessage("Wallet created successfully");
    }
}
```

---

## Dependency Usage Matrix

Bảng cho thấy service nào dùng library nào:

| Library | gateway | user-auth | wallet | transaction | reporting | notification |
|---------|:-------:|:---------:|:------:|:-----------:|:---------:|:------------:|
| `fpm-domain` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `fpm-core` | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| `fpm-common` | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| `fpm-security` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `fpm-grpc` | — | ✅ | ✅ | ✅ | ✅ | — |
| `fpm-proto` | — | ✅ | ✅ | ✅ | ✅ | — |
| `fpm-messaging` | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| `fpm-testing` | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| `fpm-bom` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

> 📌 **Tiếp theo:** Xem `05_DATABASE_SCHEMA.md` để hiểu schema của từng database.
