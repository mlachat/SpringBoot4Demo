package com.example.elstar.jms;

import com.example.elstar.entity.ElstarData;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ElstarDataMessageConverter implements MessageConverter {

    @Override
    public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
        if (!(object instanceof ElstarData)) {
            throw new MessageConversionException("Expected ElstarData but got: " +
                    (object != null ? object.getClass().getName() : "null"));
        }

        ElstarData elstarData = (ElstarData) object;
        TextMessage textMessage = session.createTextMessage();

        textMessage.setText(elstarData.getXmlNachricht());

        if (elstarData.getUuid() != null) {
            textMessage.setJMSCorrelationID(elstarData.getUuid().toString());
        }

        return textMessage;
    }

    @Override
    public Object fromMessage(Message message) throws JMSException, MessageConversionException {
        if (!(message instanceof TextMessage)) {
            throw new MessageConversionException("Expected TextMessage but got: " +
                    message.getClass().getName());
        }

        TextMessage textMessage = (TextMessage) message;
        ElstarData elstarData = new ElstarData();

        elstarData.setXmlNachricht(textMessage.getText());

        String correlationId = textMessage.getJMSCorrelationID();
        if (correlationId != null && !correlationId.isEmpty()) {
            try {
                elstarData.setUuid(UUID.fromString(correlationId));
            } catch (IllegalArgumentException e) {
                // Correlation ID is not a valid UUID, leave uuid as null
            }
        }

        return elstarData;
    }
}
