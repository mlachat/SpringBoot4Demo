package com.example.elstar.dto;

import java.util.UUID;

public class StatusUpdate {

    private final UUID uuid;
    private final Integer status;

    public StatusUpdate(UUID uuid, Integer status) {
        this.uuid = uuid;
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "StatusUpdate{uuid=" + uuid + ", status=" + status + '}';
    }
}