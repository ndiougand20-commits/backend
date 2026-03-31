package com.rezo.backend.dto.user;

import java.util.Map;

public class UpdateUserRequest {

    private String email;
    private String prenom;
    private String nom;
    private String telephone;
    private String avatarUrl;
    private Map<String, Object> profil;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Map<String, Object> getProfil() { return profil; }
    public void setProfil(Map<String, Object> profil) { this.profil = profil; }
}
