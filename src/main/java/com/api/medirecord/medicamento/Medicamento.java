package com.api.medirecord.medicamento;

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
 * Medicamento registrado por un usuario dentro de su tratamiento.
 *
 * <p>Cada registro pertenece exclusivamente al usuario identificado por
 * {@code usuarioId} (extraido del token JWT). El borrado es logico
 * ({@code activo = false}) para conservar el historial de tratamientos.</p>
 */
@Entity
@Table(name = "medicamento")
public class Medicamento extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "usuario_id", nullable = false, updatable = false, length = 100)
	private String usuarioId;

	@Column(name = "nombre", nullable = false, length = 120)
	private String nombre;

	@Column(name = "dosis", length = 100)
	private String dosis;

	@Column(name = "frecuencia", length = 100)
	private String frecuencia;

	@Column(name = "instrucciones", length = 500)
	private String instrucciones;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin")
	private LocalDate fechaFin;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Medicamento() {
		// constructor para JPA
	}

	public Medicamento(String usuarioId, String nombre, String dosis, String frecuencia,
			String instrucciones, LocalDate fechaInicio, LocalDate fechaFin) {
		this.usuarioId = usuarioId;
		this.nombre = nombre;
		this.dosis = dosis;
		this.frecuencia = frecuencia;
		this.instrucciones = instrucciones;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.activo = true;
	}

	public UUID getId() {
		return id;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDosis() {
		return dosis;
	}

	public void setDosis(String dosis) {
		this.dosis = dosis;
	}

	public String getFrecuencia() {
		return frecuencia;
	}

	public void setFrecuencia(String frecuencia) {
		this.frecuencia = frecuencia;
	}

	public String getInstrucciones() {
		return instrucciones;
	}

	public void setInstrucciones(String instrucciones) {
		this.instrucciones = instrucciones;
	}

	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(LocalDate fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public LocalDate getFechaFin() {
		return fechaFin;
	}

	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
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

	public boolean perteneceAUsuario(String usuarioAutenticado) {
		return this.usuarioId != null && this.usuarioId.equals(usuarioAutenticado);
	}

}
