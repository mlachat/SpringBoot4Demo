package com.example.elstar.batch;

import com.example.elstar.entity.ElstarData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jms.core.JmsTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueueWriterTest {

    private static final String QUEUE_NAME = "TEST.QUEUE";

    private List<Object> sentMessages;
    private TestJmsTemplate testJmsTemplate;
    private QueueWriter<ElstarData> queueWriter;

    @BeforeEach
    void setUp() {
        sentMessages = new ArrayList<>();
        testJmsTemplate = new TestJmsTemplate(sentMessages);
        queueWriter = new QueueWriter<>(testJmsTemplate, QUEUE_NAME);
    }

    @Test
    void testWriteSingleItem() throws Exception {
        ElstarData item = new ElstarData("<xml>Test</xml>");
        item.setId(1L);
        Chunk<ElstarData> chunk = new Chunk<>(item);

        queueWriter.write(chunk);

        assertEquals(1, sentMessages.size());
        assertEquals(item, sentMessages.get(0));
    }

    @Test
    void testWriteMultipleItems() throws Exception {
        ElstarData item1 = new ElstarData("<xml>Test 1</xml>");
        item1.setId(1L);
        ElstarData item2 = new ElstarData("<xml>Test 2</xml>");
        item2.setId(2L);
        ElstarData item3 = new ElstarData("<xml>Test 3</xml>");
        item3.setId(3L);
        Chunk<ElstarData> chunk = new Chunk<>(item1, item2, item3);

        queueWriter.write(chunk);

        assertEquals(3, sentMessages.size());
        assertEquals("<xml>Test 1</xml>", ((ElstarData) sentMessages.get(0)).getXmlNachricht());
        assertEquals("<xml>Test 2</xml>", ((ElstarData) sentMessages.get(1)).getXmlNachricht());
        assertEquals("<xml>Test 3</xml>", ((ElstarData) sentMessages.get(2)).getXmlNachricht());
    }

    @Test
    void testWriteEmptyChunk() throws Exception {
        Chunk<ElstarData> chunk = new Chunk<>();

        queueWriter.write(chunk);

        assertTrue(sentMessages.isEmpty());
    }

    @Test
    void testWritePreservesOrder() throws Exception {
        Chunk<ElstarData> chunk = new Chunk<>();
        for (int i = 0; i < 10; i++) {
            ElstarData item = new ElstarData("<xml>Test " + i + "</xml>");
            item.setId((long) i);
            chunk.add(item);
        }

        queueWriter.write(chunk);

        assertEquals(10, sentMessages.size());
        for (int i = 0; i < 10; i++) {
            ElstarData sent = (ElstarData) sentMessages.get(i);
            assertEquals("<xml>Test " + i + "</xml>", sent.getXmlNachricht());
            assertEquals((long) i, sent.getId());
        }
    }

    @Test
    void testGetJmsTemplate() {
        assertSame(testJmsTemplate, queueWriter.getJmsTemplate());
    }

    @Test
    void testGetDestinationName() {
        assertEquals(QUEUE_NAME, queueWriter.getDestinationName());
    }

    // Simple test stub for JmsTemplate
    private static class TestJmsTemplate extends JmsTemplate {
        private final List<Object> sentMessages;

        TestJmsTemplate(List<Object> sentMessages) {
            this.sentMessages = sentMessages;
        }

        @Override
        public void convertAndSend(String destinationName, Object message) {
            sentMessages.add(message);
        }
    }
}
