package com.example.elstar.integration;

import com.example.elstar.ReceiveBatchApplication;
import com.example.elstar.dto.StatusUpdate;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.jms.StatusUpdateMessageConverter;
import com.example.elstar.repository.ElstarDataRepository;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that verifies the complete flow:
 * 1. Send status update message to JMS queue (UUID in header, status in body)
 * 2. Batch job reads message from queue
 * 3. Entity is looked up by UUID in database
 * 4. Entity status is updated with the status from the message
 */
@SpringBootTest(classes = {ReceiveBatchApplication.class, QueueToDbIntegrationTest.IbmMqTestConfiguration.class})
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        com.ibm.mq.spring.boot.MQAutoConfiguration.class
})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Testcontainers
class QueueToDbIntegrationTest {

    private static final int MQ_PORT = 1414;
    private static final String MQ_QMGR_NAME = "QM1";
    private static final String MQ_CHANNEL = "DEV.APP.SVRCONN";
    private static final String MQ_USER = "app";
    private static final String MQ_PASSWORD = "passw0rd";

    // Test UUIDs matching test-data.sql
    private static final UUID UUID_RECORD_1 = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID UUID_RECORD_2 = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    // Status constants
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSED = 1;
    private static final int STATUS_ERROR = 2;
    private static final int STATUS_RETRY = 3;

    @Container
    static GenericContainer<?> mqContainer = new GenericContainer<>(
            DockerImageName.parse("icr.io/ibm-messaging/mq:latest"))
            .withExposedPorts(MQ_PORT)
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", MQ_QMGR_NAME)
            .withEnv("MQ_APP_PASSWORD", MQ_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*Started web server.*", 1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ibm.mq.queueManager", () -> MQ_QMGR_NAME);
        registry.add("ibm.mq.channel", () -> MQ_CHANNEL);
        registry.add("ibm.mq.connName", () -> mqContainer.getHost() + "(" + mqContainer.getMappedPort(MQ_PORT) + ")");
        registry.add("ibm.mq.user", () -> MQ_USER);
        registry.add("ibm.mq.password", () -> MQ_PASSWORD);
    }

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    private ElstarDataRepository repository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${elstar.jms.queue-name}")
    private String queueName;

    @BeforeEach
    void setUp() {
        // Drain any messages from previous tests
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(queueName) != null) {
            // drain the queue
        }
    }

    @Test
    @DisplayName("Full integration: Queue message updates database entity status")
    void whenMessageReceivedFromQueue_thenEntityStatusIsUpdatedInDatabase() throws Exception {
        // GIVEN: An entity exists in the database with status PENDING
        ElstarData entityBefore = repository.findByUuid(UUID_RECORD_1).orElseThrow();
        assertEquals(STATUS_PENDING, entityBefore.getStatus(), "Initial status should be PENDING");

        // WHEN: A status update message is sent to the queue
        StatusUpdate statusUpdate = new StatusUpdate(UUID_RECORD_1, STATUS_PROCESSED);
        jmsTemplate.convertAndSend(queueName, statusUpdate);

        // AND: The batch job processes the queue
        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        // THEN: The job completes successfully
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // AND: The entity status in the database is updated
        ElstarData entityAfter = repository.findByUuid(UUID_RECORD_1).orElseThrow();
        assertEquals(STATUS_PROCESSED, entityAfter.getStatus(), "Status should be updated to PROCESSED");
    }

    @Test
    @DisplayName("Multiple messages update multiple entities with different statuses")
    void whenMultipleMessagesReceived_thenEachEntityIsUpdatedWithCorrectStatus() throws Exception {
        // GIVEN: Two entities exist in the database with status PENDING
        assertEquals(STATUS_PENDING, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus());
        assertEquals(STATUS_PENDING, repository.findByUuid(UUID_RECORD_2).orElseThrow().getStatus());

        // WHEN: Different status update messages are sent for each entity
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(UUID_RECORD_1, STATUS_PROCESSED));
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(UUID_RECORD_2, STATUS_ERROR));

        // AND: The batch job processes the queue
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // THEN: The job completes successfully
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // AND: Each entity has the correct status from its message
        assertEquals(STATUS_PROCESSED, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus(),
                "Record 1 should have STATUS_PROCESSED");
        assertEquals(STATUS_ERROR, repository.findByUuid(UUID_RECORD_2).orElseThrow().getStatus(),
                "Record 2 should have STATUS_ERROR");
    }

    @Test
    @DisplayName("Message with unknown UUID does not affect database")
    void whenMessageHasUnknownUuid_thenNoDatabaseChanges() throws Exception {
        // GIVEN: A UUID that does not exist in the database
        UUID unknownUuid = UUID.randomUUID();
        assertTrue(repository.findByUuid(unknownUuid).isEmpty(), "UUID should not exist");

        // AND: Existing entities have PENDING status
        int initialStatus = repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus();

        // WHEN: A status update message with unknown UUID is sent
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(unknownUuid, STATUS_PROCESSED));

        // AND: The batch job processes the queue
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // THEN: The job completes successfully (no error)
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // AND: Existing entities are unchanged
        assertEquals(initialStatus, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus(),
                "Existing entity should not be affected");
    }

    @Test
    @DisplayName("Status can be updated multiple times")
    void whenStatusUpdatedMultipleTimes_thenLatestStatusIsPersisted() throws Exception {
        // GIVEN: An entity with PENDING status
        assertEquals(STATUS_PENDING, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus());

        // WHEN: First update to PROCESSED
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(UUID_RECORD_1, STATUS_PROCESSED));
        jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // THEN: Status is PROCESSED
        assertEquals(STATUS_PROCESSED, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus());

        // WHEN: Second update to ERROR
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(UUID_RECORD_1, STATUS_ERROR));
        jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // THEN: Status is ERROR
        assertEquals(STATUS_ERROR, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus());

        // WHEN: Third update to RETRY
        jmsTemplate.convertAndSend(queueName, new StatusUpdate(UUID_RECORD_1, STATUS_RETRY));
        jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // THEN: Status is RETRY
        assertEquals(STATUS_RETRY, repository.findByUuid(UUID_RECORD_1).orElseThrow().getStatus());
    }

    @TestConfiguration
    static class IbmMqTestConfiguration {

        @Bean
        @Primary
        public ConnectionFactory connectionFactory() throws JMSException {
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setHostName(mqContainer.getHost());
            factory.setPort(mqContainer.getMappedPort(MQ_PORT));
            factory.setQueueManager(MQ_QMGR_NAME);
            factory.setChannel(MQ_CHANNEL);
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setStringProperty(WMQConstants.USERID, MQ_USER);
            factory.setStringProperty(WMQConstants.PASSWORD, MQ_PASSWORD);
            return factory;
        }

        @Bean
        @Primary
        public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, StatusUpdateMessageConverter messageConverter) {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.setMessageConverter(messageConverter);
            jmsTemplate.setReceiveTimeout(1000);
            return jmsTemplate;
        }
    }
}