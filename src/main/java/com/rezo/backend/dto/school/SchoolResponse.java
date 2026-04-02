package com.rezo.backend.dto.school;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class SchoolResponse {

    private UUID id;
    private UUID ownerUserId;
    private String nomEtablissement;
    private String statut;
    private Set<String> domaines;
    private Set<String> diplomesDelivres;
    private String description;
    private String adresse;
    private String siteWeb;
    private String logoUrl;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getNomEtablissement() {
        return nomEtablissement;
    }

    public void setNomEtablissement(String nomEtablissement) {
        this.nomEtablissement = nomEtablissement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Set<String> getDomaines() {
        return domaines;
    }

    public void setDomaines(Set<String> domaines) {
        this.domaines = domaines;
    }

    public Set<String> getDiplomesDelivres() {
        return diplomesDelivres;
    }

    public void setDiplomesDelivres(Set<String> diplomesDelivres) {
        this.diplomesDelivres = diplomesDelivres;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
