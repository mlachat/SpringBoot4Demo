package com.example.elstar.batch;

import com.example.elstar.TestBatchApplication;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.jms.ElstarDataMessageConverter;
import com.example.elstar.repository.ElstarDataRepository;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(classes = {TestBatchApplication.class, ElstarJobIntegrationTest.IbmMqTestConfiguration.class})
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        com.ibm.mq.spring.boot.MQAutoConfiguration.class
})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Testcontainers
class ElstarJobIntegrationTest {

    private static final int MQ_PORT = 1414;
    private static final String MQ_QMGR_NAME = "QM1";
    private static final String MQ_CHANNEL = "DEV.APP.SVRCONN";
    private static final String MQ_USER = "app";
    private static final String MQ_PASSWORD = "passw0rd";

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
        public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, ElstarDataMessageConverter messageConverter) {
            JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
            jmsTemplate.setMessageConverter(messageConverter);
            return jmsTemplate;
        }
    }
}