# SpringBoot4Demo

A compact multi-module Maven demo showing how to combine Spring Batch, message queues, and Spring Data repositories in a modular Spring Boot project.

Purpose
- Demonstrates batch jobs (Spring Batch), asynchronous work via queues (Rabbit/Kafka/SQS), and persistence with Spring Data repositories.
- Shows a modular structure so components can be developed and tested independently.

Typical modules
- parent, common, domain/model, data (Spring Data), batch, queue, app/service, integration-tests

Quick start
Prereqs: JDK 17+, Maven, DB (Postgres/H2), message broker (RabbitMQ/Kafka) or Docker.
1. Clone: git clone https://github.com/mlachat/SpringBoot4Demo.git
2. Build: mvn clean install
3. Run (example): mvn -pl batch spring-boot:run -Dspring-boot.run.profiles=local

Notes
- Configure DB and broker in application.yml / application-{profile}.yml.
- Use JobParameters for job uniqueness; prefer outbox pattern when mixing DB + messaging.
- Test with H2/local broker or Testcontainers for full integration.

License
This project is released under the MIT License — see LICENSE for details.