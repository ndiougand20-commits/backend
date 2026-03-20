package com.rezo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String niveauEtude;

    @Column(length = 120)
    private String domaine;

    @ElementCollection
    @CollectionTable(name = "profile_competences", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "competence")
    private Set<String> competences = new HashSet<>();

    @Column(length = 150)
    private String objectif;

    @ElementCollection
    @CollectionTable(name = "profile_preferences_secteur", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "secteur")
    private Set<String> preferencesSecteur = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_preferences_lieu", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "lieu")
    private Set<String> preferencesLieu = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_experiences", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "experience")
    private Set<String> experiences = new HashSet<>();

    @Column(length = 80)
    private String classeActuelle;

    @Column(length = 80)
    private String serieOrientation;

    @Column(length = 150)
    private String objectifPostbac;

    @ElementCollection
    @CollectionTable(name = "profile_centres_interet", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "interet")
    private Set<String> centresInteret = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNiveauEtude() {
        return niveauEtude;
    }

    public void setNiveauEtude(String niveauEtude) {
        this.niveauEtude = niveauEtude;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public Set<String> getCompetences() {
        return competences;
    }

    public void setCompetences(Set<String> competences) {
        this.competences = competences;
    }

    public String getObjectif() {
        return objectif;
    }

    public void setObjectif(String objectif) {
        this.objectif = objectif;
    }

    public Set<String> getPreferencesSecteur() {
        return preferencesSecteur;
    }

    public void setPreferencesSecteur(Set<String> preferencesSecteur) {
        this.preferencesSecteur = preferencesSecteur;
    }

    public Set<String> getPreferencesLieu() {
        return preferencesLieu;
    }

    public void setPreferencesLieu(Set<String> preferencesLieu) {
        this.preferencesLieu = preferencesLieu;
    }

    public Set<String> getExperiences() {
        return experiences;
    }

    public void setExperiences(Set<String> experiences) {
        this.experiences = experiences;
    }

    public String getClasseActuelle() {
        return classeActuelle;
    }

    public void setClasseActuelle(String classeActuelle) {
        this.classeActuelle = classeActuelle;
    }

    public String getSerieOrientation() {
        return serieOrientation;
    }

    public void setSerieOrientation(String serieOrientation) {
        this.serieOrientation = serieOrientation;
    }

    public String getObjectifPostbac() {
        return objectifPostbac;
    }

    public void setObjectifPostbac(String objectifPostbac) {
        this.objectifPostbac = objectifPostbac;
    }

    public Set<String> getCentresInteret() {
        return centresInteret;
    }

    public void setCentresInteret(Set<String> centresInteret) {
        this.centresInteret = centresInteret;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
