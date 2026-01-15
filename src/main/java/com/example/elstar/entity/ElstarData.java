package com.example.elstar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "elstar_daten")
public class ElstarData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "xml_nachricht", columnDefinition = "TEXT")
    private String xmlNachricht;

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

    @Override
    public String toString() {
        return "ElstarDaten{" +
                "id=" + id +
                ", xmlNachricht='" + (xmlNachricht != null ? xmlNachricht.substring(0, Math.min(50, xmlNachricht.length())) + "..." : null) + '\'' +
                '}';
    }
}
