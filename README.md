# SpringBoot4Demo

A compact multi-module Maven demo showing how to combine Spring Batch, message queues, and Spring Data repositories in a modular Spring Boot project.

## Purpose
- Demonstrates batch jobs (Spring Batch), asynchronous work via queues (IBM MQ), and persistence with Spring Data repositories.
- Shows a modular structure so components can be developed and tested independently.

## Architecture Overview

```mermaid
flowchart LR
    subgraph "elstar-sender-batch"
        DB1[(Database)]
        SJ[elstarJob]
    end

    subgraph "JMS"
        Q[IBM MQ Queue]
    end

    subgraph "elstar-receive-batch"
        RJ[elstarReceiveJob]
        DB2[(Database)]
    end

    DB1 -->|read ElstarData| SJ
    SJ -->|send message| Q
    Q -->|receive StatusUpdate| RJ
    RJ -->|update status by UUID| DB2
```

## Batch Modules

### elstar-sender-batch

Reads `ElstarData` entities from the database and sends them to a JMS queue.

```mermaid
flowchart TB
    subgraph "elstarJob"
        subgraph "elstarStep (chunk=10)"
            R[JpaCursorItemReader]
            CW[CompositeItemWriter]

            subgraph "Writers"
                QW[QueueWriter]
                RW[RepositoryItemWriter]
            end
        end
    end

    DB[(Database<br/>ElstarData)]
    MQ[IBM MQ<br/>Queue]

    DB -->|"SELECT e FROM ElstarData e"| R
    R -->|ElstarData| CW
    CW --> QW
    CW --> RW
    QW -->|"UUID in correlation ID<br/>XML in body"| MQ
    RW -->|save| DB

    style R fill:#e1f5fe
    style QW fill:#fff3e0
    style RW fill:#e8f5e9
    style MQ fill:#fff3e0
    style DB fill:#e8f5e9
```

**Components:**
- **JpaCursorItemReader**: Reads all `ElstarData` entities from the database
- **CompositeItemWriter**: Delegates to multiple writers
  - **QueueWriter**: Sends entity to JMS queue (UUID as correlation ID)
  - **RepositoryItemWriter**: Persists entity back to database

---

### elstar-receive-batch

Reads status update messages from a JMS queue and updates corresponding entities in the database.

```mermaid
flowchart TB
    subgraph "elstarReceiveJob"
        subgraph "elstarReceiveStep (chunk=10)"
            QR[QueueReader]
            SW[StatusUpdateWriter]
        end
    end

    MQ[IBM MQ<br/>Queue]
    DB[(Database<br/>ElstarData)]

    MQ -->|"UUID from correlation ID<br/>Status from body"| QR
    QR -->|StatusUpdate| SW
    SW -->|"UPDATE status<br/>WHERE uuid = ?"| DB

    style QR fill:#fff3e0
    style SW fill:#e8f5e9
    style MQ fill:#fff3e0
    style DB fill:#e8f5e9
```

**Components:**
- **QueueReader**: Reads `StatusUpdate` messages from JMS queue
  - UUID extracted from JMS correlation ID
  - Status extracted from message body
- **StatusUpdateWriter**: Updates `ElstarData.status` in database by UUID

---

## Message Format

```mermaid
flowchart LR
    subgraph "JMS Message"
        H[Header<br/>JMSCorrelationID = UUID]
        B[Body<br/>Status Integer]
    end

    H --> MC[StatusUpdateMessageConverter]
    B --> MC
    MC --> SU[StatusUpdate DTO<br/>uuid + status]
```

## Quick Start

**Prerequisites:** JDK 17+, Maven, Docker (for IBM MQ Testcontainer)

1. Clone: `git clone https://github.com/mlachat/SpringBoot4Demo.git`
2. Build: `mvn clean install`
3. Run sender: `mvn -pl elstar-sender-batch spring-boot:run`
4. Run receiver: `mvn -pl elstar-receive-batch spring-boot:run`

## Testing

Integration tests use Testcontainers with IBM MQ:

```bash
# Run all tests
mvn test

# Run specific module tests
mvn test -pl elstar-sender-batch
mvn test -pl elstar-receive-batch
```

## Notes
- Configure DB and broker in `application.yml` / `application-{profile}.yml`
- Use JobParameters for job uniqueness
- Tests use H2 database and IBM MQ Testcontainer

## License
This project is released under the MIT License — see LICENSE for details.