package com.api.medirecord.medicamento;

import java.util.UUID;

import com.api.medirecord.common.persistence.AuditableEntity;
import com.api.medirecord.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Medicamento registrado por un usuario.
 *
 * <p>Es el lado propietario de la relacion 1:N con {@link Usuario}: la clave
 * foranea {@code usuario_id} vive aqui. La asociacion es unidireccional para no
 * acoplar el modulo de usuarios al de medicamentos; para obtener los
 * medicamentos de un usuario se usa
 * {@link MedicamentoRepository#findByUsuarioId(UUID)}.</p>
 */
@Entity
@Table(name = "medicamento")
public class Medicamento extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "nombre", nullable = false, length = 120)
	private String nombre;

	@Column(name = "dosis", length = 80)
	private String dosis;

	@Column(name = "frecuencia", length = 80)
	private String frecuencia;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Medicamento() {
		// constructor para JPA
	}

	public Medicamento(Usuario usuario, String nombre, String dosis, String frecuencia) {
		this.usuario = usuario;
		this.nombre = nombre;
		this.dosis = dosis;
		this.frecuencia = frecuencia;
		this.activo = true;
	}

	public UUID getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
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
