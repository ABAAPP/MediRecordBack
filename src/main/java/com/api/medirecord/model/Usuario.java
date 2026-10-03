package com.api.medirecord.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, length = 100)
    private String keycloakId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 20)
    private String rol = "USER";

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", updatable = false, nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Usuario() {
    }

    public Usuario(Long id, String keycloakId, String nombre, String email, String telefono, String rol, Boolean activo, LocalDateTime creadoEn) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.rol = rol;
        this.activo = activo;
        this.creadoEn = creadoEn != null ? creadoEn : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
