# SchoolApplication Spring Data JPA

📘 The project is built using Spring Data JPA, which provides a clean abstraction over JPA and handles most of the database interaction automatically through repositories.

## 📦 Features

This application provides a simple CLI interface for managing school data:

- 🔍 View all groups with a student count less than or equal to a specified number
- 📚 List all students enrolled in a specific course
- ➕ Add a new student to a group
- ❌ Delete a student by ID
- 🔗 Assign a student to a course
- 🔓 Remove a student from a course

## 🧱 Tech Stack

- Java 21  
- Spring Boot 3.4  
- Hibernate / JPA  
- Flyway  
- PostgreSQL  
- HikariCP for connection pooling  
- Docker + Docker Compose

## 🐳 Dockerized Deployment

The project is containerized with Docker. The PostgreSQL service runs in a container, and the application is built and executed inside another container.

### 🔧 Available Commands

- `.\run.ps1`  
  Rebuilds the Docker image and runs the app container interactively (no need to build the JAR manually).

- `docker-compose up -d`  
  Starts the PostgreSQL container in the background.

- `docker-compose down`  
  Stops containers but preserves database volume data.

- `docker-compose down -v`  
  Stops containers and removes database volumes (resets DB state).

- `docker-compose run --rm app`  
  Runs the app container interactively and removes it after execution.

