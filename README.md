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

The application requires PostgreSQL and can be run in two ways:

1. Run the published image from Docker Hub without cloning the repository.
2. Build and run the application locally from the source code.

### Option 1: Run from Docker Hub

This option is intended for quickly trying the released application. The source repository is not required.

The commands below are intended for Bash or WSL.

#### 1. Pull the released application image

```bash
IMAGE=yuriikorolkov/school-application-spring-data-jpa:1.0.0

docker pull "$IMAGE"
```

The immutable version tag `1.0.0` is recommended for reproducible runs. The `latest` tag points to the most recently published release.

#### 2. Create a Docker network

```bash
docker network create school-data-jpa-demo
```

#### 3. Start PostgreSQL

```bash
docker run -d --rm \
  --name school-data-jpa-postgres \
  --network school-data-jpa-demo \
  -e POSTGRES_DB=school_console_app \
  -e POSTGRES_USER=school_demo \
  -e POSTGRES_PASSWORD=local-demo-password \
  --health-cmd="pg_isready -U school_demo -d school_console_app" \
  --health-interval=5s \
  --health-timeout=5s \
  --health-retries=10 \
  postgres:16
```

#### 4. Wait until PostgreSQL is ready

```bash
until docker inspect \
  -f '{{.State.Health.Status}}' \
  school-data-jpa-postgres \
  | grep -q '^healthy$'; do
  echo "Waiting for PostgreSQL..."
  sleep 2
done
```

#### 5. Run the application

```bash
docker run --rm -it \
  --name school-data-jpa-app \
  --network school-data-jpa-demo \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://school-data-jpa-postgres:5432/school_console_app \
  -e SPRING_DATASOURCE_USERNAME=school_demo \
  -e SPRING_DATASOURCE_PASSWORD=local-demo-password \
  "$IMAGE"
```

The application starts in interactive console mode. Select `q` to exit.

#### 6. Clean up the demo environment

```bash
docker stop school-data-jpa-postgres
docker network rm school-data-jpa-demo
```

The PostgreSQL container uses temporary storage in this demo, so its data is removed during cleanup.

---

### Option 2: Build and run locally

This option is intended for development and testing changes made to the source code.

The local startup scripts automatically:

* build the executable Spring Boot JAR;
* optionally run Maven tests;
* validate the Docker Compose configuration;
* build the application Docker image;
* start PostgreSQL;
* run the application in interactive console mode.

Maven tests are skipped by default to make repeated local startup faster.

#### Linux or WSL

Make the script executable after cloning the repository if necessary:

```bash
chmod +x run.sh
```

##### Standard startup

```bash
./run.sh
```

##### Startup with Maven tests

```bash
./run.sh --run-tests
```

##### Startup with a clean database

```bash
./run.sh --reset-database
```

##### Startup with a custom PostgreSQL password

```bash
./run.sh --postgres-password "my-local-password"
```

##### Run tests and reset the database

```bash
./run.sh \
  --run-tests \
  --reset-database
```

##### Reset the database and use a custom password

```bash
./run.sh \
  --reset-database \
  --postgres-password "my-local-password"
```

##### Use all available startup options

```bash
./run.sh \
  --run-tests \
  --reset-database \
  --postgres-password "my-local-password"
```

##### Display all supported options

```bash
./run.sh --help
```

---

#### Windows PowerShell

PowerShell may prevent local scripts from running because of the current execution policy. The script can be started for the current invocation without permanently changing the system policy:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

If local scripts are already allowed, use the shorter commands below.

##### Standard startup

```powershell
.\run.ps1
```

##### Startup with Maven tests

```powershell
.\run.ps1 -RunTests
```

##### Startup with a clean database

```powershell
.\run.ps1 -ResetDatabase
```

##### Startup with a custom PostgreSQL password

```powershell
.\run.ps1 -PostgresPassword "my-local-password"
```

##### Run tests and reset the database

```powershell
.\run.ps1 `
  -RunTests `
  -ResetDatabase
```

##### Reset the database and use a custom password

```powershell
.\run.ps1 `
  -ResetDatabase `
  -PostgresPassword "my-local-password"
```

##### Use all available startup options

```powershell
.\run.ps1 `
  -RunTests `
  -ResetDatabase `
  -PostgresPassword "my-local-password"
```

The PowerShell options can also be provided on one line:

```powershell
.\run.ps1 -RunTests -ResetDatabase -PostgresPassword "my-local-password"
```

### Local environment management

After the console application exits, PostgreSQL remains available and its data is preserved for the next startup.

#### Stop the local environment

Linux or WSL:

```bash
POSTGRES_PASSWORD=local-dev-password \
  docker compose down
```

Windows PowerShell:

```powershell
$env:POSTGRES_PASSWORD = "local-dev-password"
docker compose down
Remove-Item Env:POSTGRES_PASSWORD
```

This stops and removes the containers and network while preserving the PostgreSQL volume.

#### Stop the environment and delete the database

Linux or WSL:

```bash
POSTGRES_PASSWORD=local-dev-password \
  docker compose down \
    --volumes \
    --remove-orphans
```

Windows PowerShell:

```powershell
$env:POSTGRES_PASSWORD = "local-dev-password"
docker compose down --volumes --remove-orphans
Remove-Item Env:POSTGRES_PASSWORD
```

This also removes the PostgreSQL volume and all locally stored application data.

The same cleanup can be performed automatically during the next startup.

Linux or WSL:

```bash
./run.sh --reset-database
```

Windows PowerShell:

```powershell
.\run.ps1 -ResetDatabase
```


