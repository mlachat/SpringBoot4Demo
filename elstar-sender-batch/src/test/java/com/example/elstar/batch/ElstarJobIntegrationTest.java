package com.example.elstar.batch;

import com.example.elstar.TestBatchApplication;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.jms.ElstarDataMessageConverter;
import com.example.elstar.repository.ElstarDataRepository;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(classes = {TestBatchApplication.class, ElstarJobIntegrationTest.ArtemisTestConfiguration.class})
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        com.ibm.mq.spring.boot.MQAutoConfiguration.class,
        org.springframework.boot.artemis.autoconfigure.ArtemisAutoConfiguration.class
})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Testcontainers
class ElstarJobIntegrationTest {

    private static final int ARTEMIS_PORT = 61616;
    private static final String ARTEMIS_USER = "artemis";
    private static final String ARTEMIS_PASSWORD = "artemis";

    @Container
    static GenericContainer<?> artemisContainer = new GenericContainer<>(
            DockerImageName.parse("apache/activemq-artemis:latest-alpine"))
            .withExposedPorts(ARTEMIS_PORT)
            .withEnv("ARTEMIS_USER", ARTEMIS_USER)
            .withEnv("ARTEMIS_PASSWORD", ARTEMIS_PASSWORD)
            .withEnv("ANONYMOUS_LOGIN", "true");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("artemis.broker-url", () ->
                "tcp://" + artemisContainer.getHost() + ":" + artemisContainer.getMappedPort(ARTEMIS_PORT));
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
    void clearQueue() {
        // Clear any messages from previous tests
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(queueName) != null) {
            // drain the queue
        }
    }

    @Test
    void testElstarJobCompletesSuccessfully() throws Exception {
        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();

        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(0, jobExecution.getAllFailureExceptions().size());
    }

    @Test
    void testElstarJobProcessesAllRecords() throws Exception {
        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();

        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Read all messages from the queue
        List<ElstarData> receivedMessages = receiveAllMessages();
        assertEquals(5, receivedMessages.size());
    }

    @Test
    void testElstarJobSendsCorrectMessages() throws Exception {
        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();

        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Read all messages from the queue and verify content
        List<ElstarData> receivedMessages = receiveAllMessages();
        assertEquals(5, receivedMessages.size());

        for (ElstarData elstarData : receivedMessages) {
            assertNotNull(elstarData.getUuid());
            assertNotNull(elstarData.getXmlNachricht());
            assertTrue(elstarData.getXmlNachricht().contains("<ElstarDaten>"));
        }
    }

    private List<ElstarData> receiveAllMessages() {
        List<ElstarData> messages = new ArrayList<>();
        jmsTemplate.setReceiveTimeout(1000);

        ElstarData message;
        while ((message = (ElstarData) jmsTemplate.receiveAndConvert(queueName)) != null) {
            messages.add(message);
        }

        return messages;
    }

    @TestConfiguration
    static class ArtemisTestConfiguration {

        @Bean
        @Primary
        public ConnectionFactory connectionFactory() {
            String brokerUrl = "tcp://" + artemisContainer.getHost() + ":" + artemisContainer.getMappedPort(ARTEMIS_PORT);
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            factory.setUser(ARTEMIS_USER);
            factory.setPassword(ARTEMIS_PASSWORD);
            return factory;
        }

        @Bean
        @Primary
        public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, ElstarDataMessageConverter messageConverter) {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.setMessageConverter(messageConverter);
            return jmsTemplate;
        }
    }
}