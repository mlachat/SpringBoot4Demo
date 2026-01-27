package com.example.elstar.batch;

import com.example.elstar.TestBatchApplication;
import com.example.elstar.entity.ElstarData;
import com.example.elstar.repository.ElstarDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestBatchApplication.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {com.ibm.mq.spring.boot.MQAutoConfiguration.class})
class JpaCursorItemReaderTest {

    @Autowired
    private ElstarDataRepository repository;

    @Autowired
    private JpaCursorItemReader<ElstarData> reader;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private ElstarData createElstarData(String xmlContent) {
        ElstarData data = new ElstarData(xmlContent);
        data.setUuid(UUID.randomUUID());
        return data;
    }

    @Test
    void testReaderReadsAllItems() throws Exception {
        repository.save(createElstarData("<xml>Test 1</xml>"));
        repository.save(createElstarData("<xml>Test 2</xml>"));
        repository.save(createElstarData("<xml>Test 3</xml>"));

        reader.open(new ExecutionContext());

        ElstarData item1 = reader.read();
        ElstarData item2 = reader.read();
        ElstarData item3 = reader.read();
        ElstarData item4 = reader.read();

        assertNotNull(item1);
        assertNotNull(item2);
        assertNotNull(item3);
        assertNull(item4);

        assertEquals("<xml>Test 1</xml>", item1.getXmlNachricht());
        assertEquals("<xml>Test 2</xml>", item2.getXmlNachricht());
        assertEquals("<xml>Test 3</xml>", item3.getXmlNachricht());

        reader.close();
    }

    @Test
    void testReaderReturnsNullForEmptyDatabase() throws Exception {
        reader.open(new ExecutionContext());

        ElstarData item = reader.read();

        assertNull(item);

        reader.close();
    }

    @Test
    void testReaderReadsItemWithLongXmlContent() throws Exception {
        String longXml = "<xml>" + "AAAA" + "</xml>";
        repository.save(createElstarData(longXml));

        reader.open(new ExecutionContext());

        ElstarData item = reader.read();

        assertNotNull(item);
        assertEquals(longXml, item.getXmlNachricht());

        reader.close();
    }
}
