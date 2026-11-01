package com.api.medirecord.toma;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.api.medirecord.common.persistence.AuditableEntity;
import com.api.medirecord.horario.Horario;
import com.api.medirecord.medicamento.Medicamento;

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
 * Entidad que registra el cumplimiento o realizacion de una toma de medicamento (B-09).
 *
 * <p>Permite registrar cuando el usuario tomo su medicamento para una fecha y horario
 * determinados, diferenciando tomas realizadas de tomas pendientes.</p>
 */
@Entity
@Table(name = "toma_medicamento")
public class TomaMedicamento extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "medicamento_id", nullable = false)
	private Medicamento medicamento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "horario_id")
	private Horario horario;

	@Column(name = "usuario_id", nullable = false, length = 100)
	private String usuarioId;

	@Column(name = "fecha_toma", nullable = false)
	private LocalDate fechaToma;

	@Column(name = "hora_toma", nullable = false, length = 20)
	private String horaToma;

	@Column(name = "realizada", nullable = false)
	private boolean realizada = true;

	@Column(name = "notas", length = 500)
	private String notas;

	protected TomaMedicamento() {
		// constructor para JPA
	}

	public TomaMedicamento(Medicamento medicamento, Horario horario, String usuarioId,
			LocalDate fechaToma, String horaToma, boolean realizada, String notas) {
		this.medicamento = Objects.requireNonNull(medicamento, "medicamento no puede ser null");
		this.horario = horario;
		this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
		this.fechaToma = fechaToma != null ? fechaToma : LocalDate.now();
		this.horaToma = horaToma;
		this.realizada = realizada;
		this.notas = notas;
	}

	public UUID getId() {
		return id;
	}

	public Medicamento getMedicamento() {
		return medicamento;
	}

	public void setMedicamento(Medicamento medicamento) {
		this.medicamento = Objects.requireNonNull(medicamento, "medicamento no puede ser null");
	}

	public Horario getHorario() {
		return horario;
	}

	public void setHorario(Horario horario) {
		this.horario = horario;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public LocalDate getFechaToma() {
		return fechaToma;
	}

	public void setFechaToma(LocalDate fechaToma) {
		this.fechaToma = fechaToma;
	}

	public String getHoraToma() {
		return horaToma;
	}

	public void setHoraToma(String horaToma) {
		this.horaToma = horaToma;
	}

	public boolean isRealizada() {
		return realizada;
	}

	public void setRealizada(boolean realizada) {
		this.realizada = realizada;
	}

	public String getNotas() {
		return notas;
	}

	public void setNotas(String notas) {
		this.notas = notas;
	}

	public void marcarRealizada(String hora) {
		this.realizada = true;
		if (hora != null && !hora.isBlank()) {
			this.horaToma = hora.trim();
		}
	}

	public void marcarPendiente() {
		this.realizada = false;
	}

	public boolean perteneceAUsuario(String usuarioAutenticado) {
		return this.usuarioId != null && this.usuarioId.equals(usuarioAutenticado);
	}

}
