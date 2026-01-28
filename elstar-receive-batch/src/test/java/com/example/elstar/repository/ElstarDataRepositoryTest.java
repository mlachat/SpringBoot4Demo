package com.example.elstar.repository;

import com.example.elstar.ReceiveBatchApplication;
import com.example.elstar.entity.ElstarData;
import jakarta.jms.ConnectionFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {ReceiveBatchApplication.class, ElstarDataRepositoryTest.MockJmsConfiguration.class})
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {com.ibm.mq.spring.boot.MQAutoConfiguration.class})
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class ElstarDataRepositoryTest {

    @TestConfiguration
    static class MockJmsConfiguration {
        @Bean
        @Primary
        public ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

    @Autowired
    private ElstarDataRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testFindAll() {
        var all = repository.findAll();
        assertEquals(5, all.size());
    }

    @Test
    void testFindByUuid() {
        UUID testUuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        Optional<ElstarData> result = repository.findByUuid(testUuid);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(testUuid, result.get().getUuid());
        assertTrue(result.get().getXmlNachricht().contains("12345"));
    }

    @Test
    void testFindByUuidNotFound() {
        UUID nonExistentUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        Optional<ElstarData> result = repository.findByUuid(nonExistentUuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateStatusByUuid() {
        UUID testUuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        // Verify initial status is 0
        Optional<ElstarData> before = repository.findByUuid(testUuid);
        assertTrue(before.isPresent());
        assertEquals(0, before.get().getStatus());

        // Update status to 1
        int updatedCount = repository.updateStatusByUuid(testUuid, 1);
        assertEquals(1, updatedCount);

        // Flush and clear persistence context to force fresh read from database
        entityManager.flush();
        entityManager.clear();

        Optional<ElstarData> after = repository.findByUuid(testUuid);
        assertTrue(after.isPresent());
        assertEquals(1, after.get().getStatus());
    }

    @Test
    void testUpdateStatusByUuidNotFound() {
        UUID nonExistentUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        int updatedCount = repository.updateStatusByUuid(nonExistentUuid, 1);

        assertEquals(0, updatedCount);
    }

    @Test
    void testSaveNewEntity() {
        ElstarData newEntity = new ElstarData();
        newEntity.setUuid(UUID.randomUUID());
        newEntity.setXmlNachricht("<ElstarDaten><PersonalNr>99999</PersonalNr></ElstarDaten>");
        newEntity.setCreationDate(LocalDate.now());
        newEntity.setStatus(0);

        ElstarData saved = repository.save(newEntity);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    void testUuidUniqueness() {
        UUID existingUuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        ElstarData duplicate = new ElstarData();
        duplicate.setUuid(existingUuid);
        duplicate.setXmlNachricht("<ElstarDaten><PersonalNr>DUPLICATE</PersonalNr></ElstarDaten>");
        duplicate.setStatus(0);

        assertThrows(Exception.class, () -> {
            repository.saveAndFlush(duplicate);
        });
    }
}