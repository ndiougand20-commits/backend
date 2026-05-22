package com.rezo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rezo.entities.enums.OfferType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferType type;

    @Column(length = 120)
    private String domaine;

    @Column(length = 150)
    private String location;

    @Column(length = 500)
    private String pdfUrl;

    @ElementCollection
    @CollectionTable(name = "offer_competences", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "competence", nullable = false)
    private Set<String> competencesRequises = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime datePublication;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_entreprise_id")
    private Company ownerEntreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_ecole_id")
    private School ownerEcole;

    @JsonIgnore
    @OneToMany(mappedBy = "offer")
    private Set<Swipe> swipes = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "offer")
    private Set<Message> messages = new HashSet<>();

    @PrePersist
    void validateOwner() {
        if ((ownerEntreprise == null && ownerEcole == null) || (ownerEntreprise != null && ownerEcole != null)) {
            throw new IllegalStateException("Une offre doit avoir exactement un owner: entreprise OU ecole.");
        }
        if (datePublication == null) {
            datePublication = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
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

    public OfferType getType() {
        return type;
    }

    public void setType(OfferType type) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Company getOwnerEntreprise() {
        return ownerEntreprise;
    }

    public void setOwnerEntreprise(Company ownerEntreprise) {
        this.ownerEntreprise = ownerEntreprise;
    }

    public School getOwnerEcole() {
        return ownerEcole;
    }

    public void setOwnerEcole(School ownerEcole) {
        this.ownerEcole = ownerEcole;
    }
}
