package com.rezo.backend.dto.offer;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class OfferResponse {

    private UUID id;
    private String titre;
    private String description;
    private String type;
    private String domaine;
    private String location;
    private String pdfUrl;
    private Set<String> competencesRequises;
    private LocalDateTime datePublication;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String ownerType;
    private UUID ownerUserId;
    private UUID ownerCompanyId;
    private UUID ownerSchoolId;
    private String ownerDisplayName;
    private String ownerLogoUrl;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Set<String> getCompetencesRequises() {
        return competencesRequises;
    }

    public void setCompetencesRequises(Set<String> competencesRequises) {
        this.competencesRequises = competencesRequises;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public UUID getOwnerCompanyId() {
        return ownerCompanyId;
    }

    public void setOwnerCompanyId(UUID ownerCompanyId) {
        this.ownerCompanyId = ownerCompanyId;
    }

    public UUID getOwnerSchoolId() {
        return ownerSchoolId;
    }

    public void setOwnerSchoolId(UUID ownerSchoolId) {
        this.ownerSchoolId = ownerSchoolId;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerLogoUrl() {
        return ownerLogoUrl;
    }

    public void setOwnerLogoUrl(String ownerLogoUrl) {
        this.ownerLogoUrl = ownerLogoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
