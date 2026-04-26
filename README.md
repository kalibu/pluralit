# Pluralit - Text File Processing Application

## Overview
Pluralit is a Spring Boot application for asynchronous processing of text files. It provides REST APIs for starting, stopping, pausing, and resuming processes, along with real-time status updates via WebSockets.

## Features
- Asynchronous file processing with batch handling
- Process control (start, stop, pause, resume)
- Real-time status updates via WebSockets
- Statistics extraction (word count, lines, characters, frequent words)
- Content summary generation
- Swagger API documentation
- Actuator health checks
- Docker support

## Architecture
- **Controller Layer**: REST endpoints using VOs
- **Service Layer**: Business logic with async processing
- **Repository Layer**: JPA repositories for data persistence
- **Entities**: Process, ProcessResult, ProcessLog
- **Async Processing**: Spring @Async with custom thread pool
- **WebSockets**: STOMP for real-time updates

## Technologies
- Java 21
- Spring Boot 4.0.6
- PostgreSQL
- H2 (for tests)
- Lombok
- SpringDoc OpenAPI (Swagger)
- WebSockets
- Docker

## Installation and Usage

### Prerequisites
- Java 21
- Maven 3.6+
- PostgreSQL (or Docker)

### Local Setup
1. Clone the repository
2. Configure PostgreSQL database
3. Update `application.yaml` with DB credentials
4. Place text files in `./data/files/` directory
5. Run `mvn clean install`
6. Run `mvn spring-boot:run`

### Docker Setup
1. Run `docker-compose up --build`
2. Access app at http://localhost:8080

### API Endpoints
- `POST /process/start` - Start new process
- `POST /process/stop/{process_id}` - Stop process
- `POST /process/pause/{process_id}` - Pause process
- `POST /process/resume/{process_id}` - Resume process
- `GET /process/status/{process_id}` - Get process status
- `GET /process/list` - List all processes
- `GET /process/results/{process_id}` - Get results

### Swagger Documentation
Access at http://localhost:8080/swagger-ui.html

### Actuator Endpoints
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info

## Running Tests
- `mvn test` - Run unit tests with H2

## Design Decisions
- Used async processing for non-blocking operations
- Batch processing to handle large file sets efficiently
- JSON storage for flexible result data
- WebSockets for real-time UI updates
- Separate entities for process state and results

## Key Findings
- Async processing improves responsiveness
- Batch size tuning affects performance
- JSON for results allows flexible querying
- WebSockets enable real-time monitoring

## Technical Considerations
- Concurrent state management with synchronized blocks
- Error handling with try-catch and logging
- Validation using Bean Validation
- Structured logging for debugging

## Developer
David Almeida Pitanguy - david.pitanguy@gmail.com
