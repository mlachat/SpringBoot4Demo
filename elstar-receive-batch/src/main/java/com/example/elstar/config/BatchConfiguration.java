package com.example.elstar.config;

import com.example.elstar.dto.StatusUpdate;
import com.example.elstar.reader.QueueReader;
import com.example.elstar.jms.StatusUpdateMessageConverter;
import com.example.elstar.repository.ElstarDataRepository;
import com.example.elstar.writer.StatusUpdateWriter;
import jakarta.jms.ConnectionFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfiguration {

    @Value("${elstar.jms.queue-name:DEV.QUEUE.RECEIVER}")
    private String queueName;

    @Value("${elstar.jms.receive-timeout:5000}")
    private long receiveTimeout;

    @Bean
    public JmsTemplate jmsTemplate(
            ConnectionFactory connectionFactory,
            StatusUpdateMessageConverter messageConverter) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setMessageConverter(messageConverter);
        jmsTemplate.setReceiveTimeout(receiveTimeout);
        return jmsTemplate;
    }

    @Bean
    public QueueReader<StatusUpdate> queueReader(JmsTemplate jmsTemplate) {
        return new QueueReader<>(jmsTemplate, queueName, StatusUpdate.class);
    }

    @Bean
    public StatusUpdateWriter statusUpdateWriter(ElstarDataRepository repository) {
        return new StatusUpdateWriter(repository);
    }

    @Bean
    public Step elstarReceiveStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            QueueReader<StatusUpdate> reader,
            StatusUpdateWriter writer) {
        return new StepBuilder("elstarReceiveStep", jobRepository)
                .<StatusUpdate, StatusUpdate>chunk(10)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job elstarReceiveJob(JobRepository jobRepository, Step elstarReceiveStep) {
        return new JobBuilder("elstarReceiveJob", jobRepository)
                .start(elstarReceiveStep)
                .build();
    }
}