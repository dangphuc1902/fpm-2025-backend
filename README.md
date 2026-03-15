# FPM Backend Project

This repository contains the backend source code for the FPM system, built using a modern **Microservices Architecture**. The system provides core functionalities including user authentication, wallet management, transactions, and reporting.

## 🚀 Features and Applications

The project is structured into distinct microservices:

*   **API Gateway (`api-gateway`)**: Serves as the single entry point for all client requests, routing them to the appropriate backend services. It includes JWT authentication filters, rate limiting, and circuit breaking capabilities.
*   **User Authentication Service (`user-auth-service`)**: Handles user registration, login, and secure session management using JWT (JSON Web Tokens) and Refresh Tokens. Provides a gRPC interface for other services to validate users.
*   **Wallet Service (`wallet-service`)**: Manages user wallets, including creation, balance inquiries, and transaction histories. Centralizes core transaction logic for the MVP.
*   **Reporting Service (`reporting_service`)**: Aggregates transaction events asynchronously (via Kafka) to generate various business reports (e.g., revenue, daily/monthly summaries).
*   **Service Registry (`eureka-server`)**: Uses Spring Cloud Netflix Eureka for service discovery, allowing microservices to locate and communicate with each other dynamically.

## 🌟 Advantages

*   **Modern Technology Stack**: Built with Java 21 and Spring Boot 3.5.x, ensuring high performance, security, and developer productivity.
*   **Scalable Microservices Architecture**: Services are loosely coupled, enabling independent scaling and deployments. Communication between services utilizes both synchronous REST/gRPC and asynchronous messaging.
*   **High Performance & Resiliency**: 
    *   **gRPC**: Used for efficient, low-latency inter-service communication.
    *   **Kafka & RabbitMQ**: Message brokering for asynchronous event-driven processing.
    *   **Redis**: Employed for distributed caching (`@Cacheable`) and rate limiting (via Resilience4j) to minimize database load.
*   **Containerized Infrastructure**: Docker and Docker Compose support for local development, providing a reproducible and consistent environment.

## 🛠 Prerequisites

Before running the project, ensure you have the following installed:

*   **Java 21**: The project requires JDK 21.
*   **Maven 3.9+**: For building and managing dependencies.
*   **Docker & Docker Compose**: For running the infrastructure (MySQL, Redis, Zookeeper, Kafka, RabbitMQ).

## 🚀 Getting Started

### 1. Start the Infrastructure

The project includes a `docker-compose.yml` file to spin up all required databases and message brokers.

```bash
docker-compose up -d mysql redis zookeeper kafka rabbitmq
```

Ensure all infrastructure containers are healthy before starting the microservices.

### 2. Build the Project

Use Maven to build the parent project and all modules:

```bash
mvn clean install -DskipTests
```

### 3. Run the Microservices

Services should be started in a specific order to ensure proper registration:

1.  **Eureka Server** (`eureka-server`): Start this first so other services can register.
2.  **API Gateway** (`api-gateway`)
3.  **User Auth Service** (`user-auth-service`)
4.  **Wallet Service** (`wallet-service`)
5.  **Reporting Service** (`reporting_service`)

You can run each service using the Spring Boot Maven Plugin:

```bash
cd <service-directory>
mvn spring-boot:run
```

Alternatively, you can run the entire stack using Docker Compose:

```bash
docker-compose up -d
```

## 📖 Development Guidelines & Standards

To maintain consistency and security, all developers must adhere to the following project standards:

### 1. Service Interface Naming Convention
The project uses a specific naming convention for services:
*   **Implementations**: Classes in the `.service` package act as the actual implementations (e.g., `WalletService`).
*   **Interfaces**: Interfaces are placed in a `.service.imp` package and end with the `Imp` suffix (e.g., `WalletServiceImp`).

### 2. Security & Configuration
*   **Externalized Secrets**: Sensitive properties like `jwt.secret` **MUST NOT** contain hardcoded default values in Java code or configuration files. They must be strictly externalized using environment variables (e.g., `${JWT_SECRET}`).
*   **Configuration Management**: Microservices are designed to import external configurations from a Spring Cloud Config Server (expected at `http://localhost:8888`).

### 3. Database Patterns
*   **Soft Deletes**: The codebase utilizes a soft-delete pattern for entities. Entities (like Wallets) use an `isDeleted` flag combined with Hibernate's `@Where(clause = "is_deleted = false")` annotation to filter out deleted records automatically.

### 4. gRPC and Protobufs
*   gRPC proto definitions (`.proto` files) are located within the `src/main/proto` directory of the defining service.

### 5. Scheduled Tasks
*   When creating any Scheduled Tasks, always add the following comment at the very beginning of the method: 
    `// Goal: Stabilize backend services and complete MVP APIs for Wallet application before Mobile integration.`

### 6. Package Naming Discrepancy Note
*   Be aware of a naming discrepancy in the `reporting_service` module: source files use the `com.fpm_2025.reportingservice` package, while test files use `com.fpm_2025.reporting_service` (with an underscore).

## 🧪 Testing
The project uses Maven for builds and testing. To run all tests across the project:

```bash
mvn test
```

## 📝 TODOs
Please refer to the [`TODO.md`](./TODO.md) file for the current roadmap, pending tasks, and integration phases.
