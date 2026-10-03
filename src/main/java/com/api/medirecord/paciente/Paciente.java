package com.api.medirecord.paciente;

import java.time.LocalDate;
import java.util.UUID;

import com.api.medirecord.common.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Paciente del expediente clinico.
 *
 * <p>Las entidades nunca se exponen directamente en la API: se convierten a
 * {@code PacienteResponse}. El borrado es logico ({@code activo = false}) por
 * tratarse de datos clinicos.</p>
 */
@Entity
@Table(name = "paciente")
public class Paciente extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Column(name = "apellido_paterno", nullable = false, length = 80)
	private String apellidoPaterno;

	@Column(name = "apellido_materno", length = 80)
	private String apellidoMaterno;

	@Column(name = "fecha_nacimiento", nullable = false)
	private LocalDate fechaNacimiento;

	@Column(name = "email", nullable = false, length = 120)
	private String email;

	@Column(name = "telefono", length = 20)
	private String telefono;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Paciente() {
		// constructor para JPA
	}

	public Paciente(String nombre, String apellidoPaterno, String apellidoMaterno,
			LocalDate fechaNacimiento, String email, String telefono) {
		this.nombre = nombre;
		this.apellidoPaterno = apellidoPaterno;
		this.apellidoMaterno = apellidoMaterno;
		this.fechaNacimiento = fechaNacimiento;
		this.email = email;
		this.telefono = telefono;
		this.activo = true;
	}

	public UUID getId() {
		return id;
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

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public void setFechaNacimiento(LocalDate fechaNacimiento) {
		this.fechaNacimiento = fechaNacimiento;
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