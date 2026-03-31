package com.rezo.backend.dto.user;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class UserMeResponse {

    private UUID id;
    private String email;
    private String prenom;
    private String nom;
    private String telephone;
    private String role;
    private String avatarUrl;
    private String packNom;
    private LocalDateTime createdAt;
    private Map<String, Object> profil;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPackNom() { return packNom; }
    public void setPackNom(String packNom) { this.packNom = packNom; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getProfil() { return profil; }
    public void setProfil(Map<String, Object> profil) { this.profil = profil; }
}
