# Grocery Manager

## Overview

Grocery Manager is a Spring Boot-based RESTful service for managing products and orders, including stock reservation, order lifecycle management, and product archiving. It is designed for reliability, extensibility, and observability, supporting features such as distributed tracing, metrics, and robust error handling.

## Architecture

The application follows a layered architecture:

- **Controller Layer**: Exposes RESTful endpoints for product and order management. Handles request validation and error mapping.
- **Service Layer**: Contains business logic for order processing, product management, and transactional operations. Implements retry and caching strategies.
- **Persistence Layer**: Uses Spring Data JPA for database access, with H2 as the default database and Liquibase for schema migrations.
- **Configuration Layer**: Centralizes configuration for security, caching, retry policies, OpenAPI documentation, and scheduling.

### Key Technologies
- **Spring Boot 3.3**: Rapid application development and dependency management.
- **Spring Data JPA**: ORM and repository abstraction.
- **Spring Security**: Basic authentication with in-memory user store.
- **Caffeine Cache**: High-performance local caching.
- **Spring Retry**: Declarative retry for transactional operations.
- **Liquibase**: Database schema versioning.
- **Micrometer & Prometheus**: Metrics and monitoring.
- **Logbook & Logstash**: HTTP and application logging.
- **OpenAPI/Swagger**: API documentation.
- **JUnit & Spring Test**: Automated testing.

## Main Components

- **Product Management**: CRUD operations, soft-deletion (archiving), and checks for active/finished orders.
- **Order Management**: Create, pay, cancel, and expire orders with stock reservation logic.
- **Scheduling**: Periodic job to expire unpaid orders based on configurable thresholds.
- **Caching**: Product and order data cached with configurable TTL and size.
- **Retry Logic**: Database transaction retries for transient errors.
- **Security**: Basic authentication with configurable users and roles.
- **Error Handling**: Centralized exception resolver for consistent API error responses.
- **Observability**: Exposes health, metrics, and tracing endpoints for monitoring.

## Trade-offs

- **In-memory Database (H2)**: Chosen for simplicity and ease of testing. For production, a persistent RDBMS (e.g., PostgreSQL) is recommended.
- **In-memory User Store**: Simple for demo and development; for real deployments, integrate with external identity providers.
- **Synchronous Processing**: All operations are synchronous for simplicity. Asynchronous/event-driven processing could improve scalability for high-throughput scenarios.
- **Local Caching**: Caffeine is fast but not distributed. For clustered deployments, Redis or another distributed cache could be used.
- **Basic Auth**: Easy to set up, but less secure than OAuth2/JWT for production APIs.
- **URI API Versioning**: Simple and clear. Header-based versioning could be considered for future flexibility.

## Possible Improvements

- **Database**: Switch to a production-grade RDBMS and externalize configuration.
- **Authentication**: Integrate OAuth2/JWT for better security and scalability.
- **Distributed Caching**: Use Redis, Infinispan or similar for multi-instance deployments.
- **Rate Limiting**: Protect endpoints from abuse - API gateway, Resilience4j can be used.
- **Async Processing**: Use messaging (Kafka, RabbitMQ) for order expiration and stock updates.
- **CI/CD**: Add pipelines for automated testing, building, and deployment.
- **Containerization**: Provided via Jib plugin; can be extended for cloud-native deployments.

## Getting Started

1. **Build and Run Locally**
   - `mvn clean package`
   - `java -jar target/grocery-manager-0.0.1-SNAPSHOT.jar`

2. **Run as a Docker Image**
   - Build the Docker image using Jib (no Docker daemon required):
     - `mvn compile com.google.cloud.tools:jib-maven-plugin:3.4.4:dockerBuild`
     - This will create a Docker image named `grocery-manager:latest`.
   - Run the container:
     - `docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=devel grocery-manager:latest`
   - The application will be accessible at [http://localhost:8080](http://localhost:8080)

3. **API Documentation**
   - Swagger UI: [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

4. **Actuator Endpoints**
   - Health: `/management/health`
   - Metrics: `/management/prometheus`

5. **Default Credentials**
   - Username: `admin`
   - Password: `admin`

## Configuration

All configuration is managed via `application.yaml` and environment variables. Key settings include database, cache, retry, scheduling, and security properties.

## Performance Testing

Performance tests are provided using [Taurus](https://gettaurus.org/), which can execute JMeter and other test types. The Taurus configuration and scenarios are located in `src/test/taurus/`.

To run the performance tests using Docker:

```bash
cd src/test/taurus
# Run Taurus with the provided configuration
docker run --network host -it --rm -v "$(pwd)":/bzt-configs -v "$(pwd)"/artifacts:/tmp/artifacts blazemeter/taurus -sequential simple.yaml
```

See `src/test/taurus/README.md` for more details.
