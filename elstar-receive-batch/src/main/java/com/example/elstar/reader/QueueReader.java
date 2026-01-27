package com.example.elstar.reader;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.jms.core.JmsTemplate;

public class QueueReader<T> implements ItemReader<T> {

    private final JmsTemplate jmsTemplate;
    private final String destinationName;
    private final Class<T> targetType;

    public QueueReader(JmsTemplate jmsTemplate, String destinationName, Class<T> targetType) {
        this.jmsTemplate = jmsTemplate;
        this.destinationName = destinationName;
        this.targetType = targetType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read() throws Exception {
        Object message = jmsTemplate.receiveAndConvert(destinationName);
        if (message == null) {
            return null;
        }
        if (!targetType.isInstance(message)) {
            throw new IllegalStateException("Expected " + targetType.getName() +
                    " but received: " + message.getClass().getName());
        }
        return (T) message;
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public String getDestinationName() {
        return destinationName;
    }
}
