package com.rezo.backend.dto.school;

import java.util.List;

public class SchoolRequest {

    private String nomEtablissement;
    private String statut;
    private List<String> domaines;
    private List<String> diplomesDelivres;
    private String description;
    private String adresse;
    private String siteWeb;
    private String logoUrl;

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

    public List<String> getDomaines() {
        return domaines;
    }

    public void setDomaines(List<String> domaines) {
        this.domaines = domaines;
    }

    public List<String> getDiplomesDelivres() {
        return diplomesDelivres;
    }

    public void setDiplomesDelivres(List<String> diplomesDelivres) {
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
}
