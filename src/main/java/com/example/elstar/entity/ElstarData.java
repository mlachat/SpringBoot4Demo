package com.example.elstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "elstar_daten")
public class ElstarData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "xml_nachricht", columnDefinition = "TEXT")
    private String xmlNachricht;

    @Column(name = "creation_date")
    private LocalDate creationDate;

    private Integer status;

    public ElstarData() {
    }

    public ElstarData(String xmlNachricht) {
        this.xmlNachricht = xmlNachricht;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getXmlNachricht() {
        return xmlNachricht;
    }

    public void setXmlNachricht(String xmlNachricht) {
        this.xmlNachricht = xmlNachricht;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ElstarDaten{" +
                "id=" + id +
                ", xmlNachricht='" + (xmlNachricht != null ? xmlNachricht.substring(0, Math.min(50, xmlNachricht.length())) + "..." : null) + '\'' +
                '}';
    }
}
