package com.example.elstar.jms;

import com.example.elstar.dto.StatusUpdate;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StatusUpdateMessageConverter implements MessageConverter {

    @Override
    public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
        if (!(object instanceof StatusUpdate)) {
            throw new MessageConversionException("Expected StatusUpdate but got: " +
                    (object != null ? object.getClass().getName() : "null"));
        }

        StatusUpdate statusUpdate = (StatusUpdate) object;
        TextMessage textMessage = session.createTextMessage();

        textMessage.setText(String.valueOf(statusUpdate.getStatus()));

        if (statusUpdate.getUuid() != null) {
            textMessage.setJMSCorrelationID(statusUpdate.getUuid().toString());
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

        // Extract UUID from correlation ID
        String correlationId = textMessage.getJMSCorrelationID();
        if (correlationId == null || correlationId.isEmpty()) {
            throw new MessageConversionException("Missing JMS correlation ID (UUID)");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(correlationId);
        } catch (IllegalArgumentException e) {
            throw new MessageConversionException("Invalid UUID in correlation ID: " + correlationId, e);
        }

        // Extract status from message body
        String statusText = textMessage.getText();
        if (statusText == null || statusText.isEmpty()) {
            throw new MessageConversionException("Missing status in message body");
        }

        Integer status;
        try {
            status = Integer.parseInt(statusText.trim());
        } catch (NumberFormatException e) {
            throw new MessageConversionException("Invalid status value: " + statusText, e);
        }

        return new StatusUpdate(uuid, status);
    }
}