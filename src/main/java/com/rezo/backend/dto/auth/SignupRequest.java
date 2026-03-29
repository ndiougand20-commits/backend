package com.rezo.backend.dto.auth;

import java.util.Map;

public class SignupRequest {

    private String email;
    private String password;
    private String role;
    private String prenom;
    private String nom;
    private String telephone;
    private Map<String, Object> profil;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Map<String, Object> getProfil() {
        return profil;
    }

    public void setProfil(Map<String, Object> profil) {
        this.profil = profil;
    }
}
