package com.example.elstar.config;

import com.example.elstar.batch.QueueWriter;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.jms.ElstarDataMessageConverter;
import com.example.elstar.repository.ElstarDataRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfiguration {

    @Value("${elstar.jms.queue-name:DEV.QUEUE.1}")
    private String queueName;

    @Bean
    public JpaCursorItemReader<ElstarData> elstarDatenReader(EntityManagerFactory entityManagerFactory) {
        return new JpaCursorItemReaderBuilder<ElstarData>()
                .name("elstarDatenReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT e FROM ElstarData e")
                .build();
    }

    @Bean
    public JmsTemplate jmsTemplate(
            jakarta.jms.ConnectionFactory connectionFactory,
            ElstarDataMessageConverter messageConverter) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setMessageConverter(messageConverter);
        return jmsTemplate;
    }

    @Bean
    public QueueWriter<ElstarData> queueWriter(JmsTemplate jmsTemplate) {
        return new QueueWriter<>(jmsTemplate, queueName);
    }

    @Bean
    public RepositoryItemWriter<ElstarData> repositoryItemWriter(ElstarDataRepository repository) {
        return new  RepositoryItemWriterBuilder<ElstarData>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public CompositeItemWriter<ElstarData> compositeWriter(
            QueueWriter<ElstarData> queueWriter,
            RepositoryItemWriter<ElstarData> repositoryItemWriter) {
        return new CompositeItemWriterBuilder<ElstarData>()
                .delegates(queueWriter, repositoryItemWriter)
                .build();
    }

    @Bean
    public Step elstarStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaCursorItemReader<ElstarData> reader,
            CompositeItemWriter<ElstarData> writer) {
        return new StepBuilder("elstarStep", jobRepository)
                .<ElstarData, ElstarData>chunk(10)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job elstarJob(JobRepository jobRepository, Step elstarStep) {
        return new JobBuilder("elstarJob", jobRepository)
                .start(elstarStep)
                .build();
    }
}
