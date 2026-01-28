package com.example.elstar.batch;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ReceiveBatchApplication.class, ElstarReceiveJobIntegrationTest.IbmMqTestConfiguration.class})
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        com.ibm.mq.spring.boot.MQAutoConfiguration.class
})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Testcontainers
class ElstarReceiveJobIntegrationTest {

    private static final int MQ_PORT = 1414;
    private static final String MQ_QMGR_NAME = "QM1";
    private static final String MQ_CHANNEL = "DEV.APP.SVRCONN";
    private static final String MQ_USER = "app";
    private static final String MQ_PASSWORD = "passw0rd";

    // Test UUIDs matching test-data.sql
    private static final UUID TEST_UUID_1 = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID TEST_UUID_2 = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID TEST_UUID_3 = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");

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
        // Clear any messages from previous tests
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(queueName) != null) {
            // drain the queue
        }
    }

    @Test
    void testElstarReceiveJobCompletesSuccessfully() throws Exception {
        // Send test messages to the queue with status 1
        sendStatusUpdate(TEST_UUID_1, 1);
        sendStatusUpdate(TEST_UUID_2, 1);
        sendStatusUpdate(TEST_UUID_3, 1);

        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(0, jobExecution.getAllFailureExceptions().size());
    }

    @Test
    void testElstarReceiveJobUpdatesStatus() throws Exception {
        // Verify initial status is 0
        Optional<ElstarData> before = repository.findByUuid(TEST_UUID_1);
        assertTrue(before.isPresent());
        assertEquals(0, before.get().getStatus());

        // Send message with status 1
        sendStatusUpdate(TEST_UUID_1, 1);

        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify status was updated to 1
        Optional<ElstarData> after = repository.findByUuid(TEST_UUID_1);
        assertTrue(after.isPresent());
        assertEquals(1, after.get().getStatus());
    }

    @Test
    void testElstarReceiveJobUpdatesWithDifferentStatuses() throws Exception {
        // Verify all initial statuses are 0
        assertEquals(0, repository.findByUuid(TEST_UUID_1).get().getStatus());
        assertEquals(0, repository.findByUuid(TEST_UUID_2).get().getStatus());
        assertEquals(0, repository.findByUuid(TEST_UUID_3).get().getStatus());

        // Send messages with different status values
        sendStatusUpdate(TEST_UUID_1, 1);  // processed
        sendStatusUpdate(TEST_UUID_2, 2);  // error
        sendStatusUpdate(TEST_UUID_3, 3);  // retry

        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify each status was updated to the value from the message
        assertEquals(1, repository.findByUuid(TEST_UUID_1).get().getStatus());
        assertEquals(2, repository.findByUuid(TEST_UUID_2).get().getStatus());
        assertEquals(3, repository.findByUuid(TEST_UUID_3).get().getStatus());
    }

    @Test
    void testElstarReceiveJobHandlesEmptyQueue() throws Exception {
        // Don't send any messages - queue is empty

        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify no statuses were updated (all should still be 0)
        List<ElstarData> allRecords = repository.findAll();
        for (ElstarData record : allRecords) {
            assertEquals(0, record.getStatus());
        }
    }

    @Test
    void testElstarReceiveJobHandlesUnknownUuid() throws Exception {
        UUID unknownUuid = UUID.randomUUID();

        // Send message with UUID that doesn't exist in database
        sendStatusUpdate(unknownUuid, 1);

        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        // Job should complete successfully even if UUID not found
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify no records were affected
        Optional<ElstarData> notFound = repository.findByUuid(unknownUuid);
        assertTrue(notFound.isEmpty());
    }

    private void sendStatusUpdate(UUID uuid, Integer status) {
        StatusUpdate statusUpdate = new StatusUpdate(uuid, status);
        jmsTemplate.convertAndSend(queueName, statusUpdate);
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