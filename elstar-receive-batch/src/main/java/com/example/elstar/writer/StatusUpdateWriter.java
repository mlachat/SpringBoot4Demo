package com.example.elstar.writer;

import com.example.elstar.dto.StatusUpdate;
import com.example.elstar.repository.ElstarDataRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

public class StatusUpdateWriter implements ItemWriter<StatusUpdate> {

    private final ElstarDataRepository repository;

    public StatusUpdateWriter(ElstarDataRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends StatusUpdate> chunk) throws Exception {
        for (StatusUpdate item : chunk) {
            if (item.getUuid() != null && item.getStatus() != null) {
                repository.updateStatusByUuid(item.getUuid(), item.getStatus());
            }
        }
    }
}