package com.api.medirecord.usuario;

import java.util.UUID;

import com.api.medirecord.common.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario final de la aplicacion: la persona que necesita recordatorios para no
 * olvidar tomarse sus medicamentos.
 *
 * <p>La autenticacion se delega a Keycloak; por eso la entidad <b>no</b>
 * almacena contrasenas. {@link #keycloakId} guarda el claim {@code sub} del
 * token y es el enlace entre la cuenta local y la identidad externa.</p>
 *
 * <p>Las entidades nunca se exponen en la API: se convierten a
 * {@code UsuarioResponse}. El borrado es logico ({@code activo = false}).</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "keycloak_id", unique = true, length = 100)
	private String keycloakId;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "apellido_paterno", nullable = false, length = 100)
	private String apellidoPaterno;

	@Column(name = "apellido_materno", length = 100)
	private String apellidoMaterno;

	@Column(name = "email", nullable = false, length = 150)
	private String email;

	@Column(name = "telefono", length = 20)
	private String telefono;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Usuario() {
		// constructor para JPA
	}

	public Usuario(String nombre, String apellidoPaterno, String apellidoMaterno,
			String email, String telefono) {
		this.nombre = nombre;
		this.apellidoPaterno = apellidoPaterno;
		this.apellidoMaterno = apellidoMaterno;
		this.email = email;
		this.telefono = telefono;
		this.activo = true;
	}

	public UUID getId() {
		return id;
	}

	public String getKeycloakId() {
		return keycloakId;
	}

	/** Vincula la cuenta con la identidad de Keycloak (claim {@code sub}). */
	public void vincularKeycloakId(String keycloakId) {
		this.keycloakId = keycloakId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellidoPaterno() {
		return apellidoPaterno;
	}

	public void setApellidoPaterno(String apellidoPaterno) {
		this.apellidoPaterno = apellidoPaterno;
	}

	public String getApellidoMaterno() {
		return apellidoMaterno;
	}

	public void setApellidoMaterno(String apellidoMaterno) {
		this.apellidoMaterno = apellidoMaterno;
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

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public void desactivar() {
		this.activo = false;
	}

}
