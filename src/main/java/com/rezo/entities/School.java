package com.rezo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rezo.entities.enums.SchoolStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String nomEtablissement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchoolStatus statut;

    @ElementCollection
    @CollectionTable(name = "school_domaines", joinColumns = @JoinColumn(name = "school_id"))
    @Column(name = "domaine")
    private Set<String> domaines = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "school_diplomes", joinColumns = @JoinColumn(name = "school_id"))
    @Column(name = "diplome")
    private Set<String> diplomesDelivres = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String adresse;

    @Column(length = 255)
    private String siteWeb;

    @Column(length = 255)
    private String logoUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "ownerEcole")
    private Set<Offer> offers = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNomEtablissement() {
        return nomEtablissement;
    }

    public void setNomEtablissement(String nomEtablissement) {
        this.nomEtablissement = nomEtablissement;
    }

    public SchoolStatus getStatut() {
        return statut;
    }

    public void setStatut(SchoolStatus statut) {
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
}
