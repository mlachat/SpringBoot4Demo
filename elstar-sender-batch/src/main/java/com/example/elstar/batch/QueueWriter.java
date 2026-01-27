package com.example.elstar.batch;


import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jms.core.JmsTemplate;

public class QueueWriter<T> implements ItemWriter<T> {

    private final JmsTemplate jmsTemplate;
    private final String destinationName;

    public QueueWriter(JmsTemplate jmsTemplate, String destinationName) {
        this.jmsTemplate = jmsTemplate;
        this.destinationName = destinationName;
    }

    @Override
    public void write(Chunk<? extends T> chunk) throws Exception {
        for (T item : chunk) {
            jmsTemplate.convertAndSend(destinationName, item);
        }
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public String getDestinationName() {
        return destinationName;
    }
}
