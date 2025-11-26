# High-Frequency Stock Ticker Ingestion Service

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-green?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?style=flat-square)
![Performance](https://img.shields.io/badge/RPS-15k+-red?style=flat-square)

## 📖 Project Overview

This is a **high-throughput, low-latency microservice** designed to ingest real-time financial stock market data. The system is engineered to handle massive concurrent traffic spikes by decoupling the HTTP ingestion layer from the database persistence layer.

Tis project moves beyond standard CRUD operations to demonstrate **resource efficiency**, **concurrency control**, and **fault tolerance**.

### Key Achievements
* **15,000+ RPS** on local hardware using Virtual Threads.
* **1,250+ RPS** on constrained "Cloud-like" resources (1 vCPU).
* **Zero Data Loss** guaranteed via Graceful Shutdown implementation.
* **99.87% Availability** demonstrated during Chaos Engineering tests.

---

## 📊 Performance Benchmarks

*Hardware Context: Standard Dev Laptop testing against Docker Resource Limits.*

| Scenario | Infrastructure | Throughput (RPS) | P95 Latency | Analysis |
| :--- | :--- | :--- | :--- | :--- |
| **Baseline** | Local JVM (12 CPU) | **~15,600 RPS** | 6.25ms | Raw application overhead is minimal. |
| **Starved** | Docker (0.5 vCPU) | ~463 RPS | 293ms | **CPU Starvation.** High Context Switching causes massive latency spikes (300ms+). |
| **Production** | Docker (1.0 vCPU) | **~1,585 RPS** | **89ms** | **The Sweet Spot.** By adding 0.5 CPU, performance increased **4x**. Efficient utilization. |
| **Unbounded** | Docker (No Limits) | ~4,335 RPS | 11ms | Shows burst capacity when CPU limits are removed but Network I/O is present. |

**Docker (0.5 vCPU)**
<img width="1015" height="491" alt="image" src="https://github.com/user-attachments/assets/17076ec8-56ac-4aee-87a0-30612399ad29" />
**Docker (1.0 vCPU)**
<img width="1047" height="562" alt="image" src="https://github.com/user-attachments/assets/45e3c2ff-563b-416f-b6fd-e03626018482" />


---

## 🛠️ Engineering Challenges & Solutions

### 1. Shifting the Bottleneck (I/O to CPU)
* **Challenge:** Direct database writes limited throughput to ~200 RPS (I/O Bound).
* **Solution:** Implemented an asynchronous `ConcurrentLinkedQueue`.
* **Result:** Throughput jumped to **1,500+ RPS**. The container now runs at **100% CPU Utilization**, proving that the bottleneck successfully shifted to request processing (JSON parsing) rather than waiting on the DB.

### 2. Resource Contention
* **Challenge:** Scaling horizontally from 1 to 3 pods on a single machine caused throughput to drop.
* **Root Cause:** `docker stats` revealed high Context Switching as the host OS thrashed between containers.
* **Takeaway:** Horizontal scaling requires physical capacity. Validated that Vertical Scaling (0.5 CPU -> 1.0 CPU) was the most efficient approach for this hardware profile.

### 3. Data Safety on Crash
* **Challenge:** Because data was buffered in RAM, a `SIGTERM` (Pod restart) would cause data loss of queued items.
* **Solution:** Added a Graceful Shutdown hook.
    ```java
    @PreDestroy
    public void onShutdown() {
        while (!buffer.isEmpty()) {
            flushBufferToDb(); // Drains the queue before JVM exits
        }
    }
    ```
* **Verification:** Verified **100% data integrity** (Row Count matched Load Test requests) even when the container was stopped mid-load-test.
  <img width="1021" height="114" alt="image" src="https://github.com/user-attachments/assets/ae499960-c149-4bd0-a996-35e82cf4691d" />


---

## 🚀 Run Locally

### Prerequisites
* Java 21+
* Docker & Docker Compose
* [k6](https://k6.io/) (for load testing)

### 1. Build the Application
```bash
mvn clean package -DskipTests
```

### 2. Run with Docker Compose
Starts the Application (Limit: 1 CPU, 512MB RAM) and PostgreSQL.
```bash
docker-compose up -d --build
```
* **App:** `http://localhost:8080`
* **DB:** Port `5432`

### 3. Run the Load Test
The project includes a custom k6 script to simulate 50 concurrent stock feed sensors.
```bash
k6 run k6/load-test.js
```

### 4. Verify Data Integrity
Check if all requests were persisted to PostgreSQL:
```bash
docker exec -it stock-postgres psql -U user -d stockdb -c "SELECT COUNT(*) FROM ticks;"
```

---

## 💻 Tech Stack Details

* **Language:** Java 21
* **Framework:** Spring Boot 4.0 (Web, JDBC)
* **Database:** PostgreSQL 15
* **Containerization:** Docker, Docker Compose
* **Testing:** k6 (Performance), Chaos Engineering
* **Key Libraries:** `spring-boot-starter-jdbc`, `lombok`

---

## 📝 Configuration Snippets

**`application.yaml` (Virtual Threads)**
```yaml
spring:
  threads:
    virtual:
      enabled: true
server:
  shutdown: graceful # Waits for active requests
```

**`docker-compose.yml` (Resource Constraints)**
```yaml
    deploy:
      resources:
        limits:
          cpus: '1.0' # Simulating Cloud vCPU
          memory: 512M
```
