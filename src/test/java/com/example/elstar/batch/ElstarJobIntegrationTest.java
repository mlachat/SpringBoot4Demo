package com.example.elstar.batch;

import com.example.elstar.TestBatchApplication;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.repository.ElstarDataRepository;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestBatchApplication.class, ElstarJobIntegrationTest.TestJmsConfiguration.class})
@SpringBatchTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {com.ibm.mq.spring.boot.MQAutoConfiguration.class})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ElstarJobIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    private ElstarDataRepository repository;

    @Autowired
    private TestJmsTemplate testJmsTemplate;

    @BeforeEach
    void setUp() {
        testJmsTemplate.clearMessages();
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
        assertEquals(5, testJmsTemplate.getSentMessages().size());
    }

    @Test
    void testElstarJobSendsCorrectMessages() throws Exception {
        JobParameters jobParameters = jobOperatorTestUtils.getUniqueJobParameters();

        JobExecution jobExecution = jobOperatorTestUtils.startJob(jobParameters);

        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        List<Object> sentMessages = testJmsTemplate.getSentMessages();
        assertEquals(5, sentMessages.size());

        for (Object message : sentMessages) {
            assertTrue(message instanceof ElstarData);
            ElstarData elstarData = (ElstarData) message;
            assertNotNull(elstarData.getId());
            assertNotNull(elstarData.getXmlNachricht());
            assertTrue(elstarData.getXmlNachricht().contains("<ElstarDaten>"));
        }
    }

    @TestConfiguration
    static class TestJmsConfiguration {

        @Bean
        @Primary
        public ConnectionFactory connectionFactory() {
            return new StubConnectionFactory();
        }

        @Bean
        @Primary
        public TestJmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
            return new TestJmsTemplate(connectionFactory);
        }
    }

    static class TestJmsTemplate extends JmsTemplate {
        private final List<Object> sentMessages = new ArrayList<>();

        public TestJmsTemplate(ConnectionFactory connectionFactory) {
            super(connectionFactory);
        }

        @Override
        public void convertAndSend(String destinationName, Object message) {
            sentMessages.add(message);
        }

        public List<Object> getSentMessages() {
            return sentMessages;
        }

        public void clearMessages() {
            sentMessages.clear();
        }
    }

    static class StubConnectionFactory implements ConnectionFactory {
        @Override
        public Connection createConnection() throws JMSException {
            throw new UnsupportedOperationException("Stub connection factory");
        }

        @Override
        public Connection createConnection(String userName, String password) throws JMSException {
            throw new UnsupportedOperationException("Stub connection factory");
        }

        @Override
        public JMSContext createContext() {
            throw new UnsupportedOperationException("Stub connection factory");
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            throw new UnsupportedOperationException("Stub connection factory");
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            throw new UnsupportedOperationException("Stub connection factory");
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            throw new UnsupportedOperationException("Stub connection factory");
        }
    }
}