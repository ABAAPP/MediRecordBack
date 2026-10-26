package com.api.medirecord.horario;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.api.medirecord.common.persistence.AuditableEntity;
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
 * Horario y frecuencia de administracion asociados a un medicamento (B-07).
 *
 * <p>Almacena la hora programada, la frecuencia de toma y los dias de la semana
 * seleccionados para alimentar al servicio de recordatorios.</p>
 */
@Entity
@Table(name = "horario")
public class Horario extends AuditableEntity {

	public static final List<String> TODOS_LOS_DIAS = List.of(
			"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

	private static final String SEPARADOR_DIAS = ",";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "medicamento_id", nullable = false)
	private Medicamento medicamento;

	@Column(name = "hora_administracion", nullable = false, length = 20)
	private String horaAdministracion;

	@Column(name = "frecuencia", nullable = false, length = 100)
	private String frecuencia;

	@Column(name = "dias_semana", nullable = false, length = 250)
	private String diasSemana;

	@Column(name = "recordatorio_activo", nullable = false)
	private boolean recordatorioActivo = true;

	@Column(name = "notas", length = 500)
	private String notas;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Horario() {
		// constructor para JPA
	}

	public Horario(Medicamento medicamento, String horaAdministracion, String frecuencia,
			List<String> diasSeleccionados, boolean recordatorioActivo, String notas) {
		this.medicamento = Objects.requireNonNull(medicamento, "medicamento no puede ser null");
		this.horaAdministracion = horaAdministracion;
		this.frecuencia = frecuencia;
		setDiasSeleccionados(diasSeleccionados);
		this.recordatorioActivo = recordatorioActivo;
		this.notas = notas;
		this.activo = true;
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

	public String getHoraAdministracion() {
		return horaAdministracion;
	}

	public void setHoraAdministracion(String horaAdministracion) {
		this.horaAdministracion = horaAdministracion;
	}

	public String getFrecuencia() {
		return frecuencia;
	}

	public void setFrecuencia(String frecuencia) {
		this.frecuencia = frecuencia;
	}

	public String getDiasSemana() {
		return diasSemana;
	}

	public List<String> getDiasSeleccionados() {
		if (diasSemana == null || diasSemana.isBlank()) {
			return TODOS_LOS_DIAS;
		}
		return Arrays.stream(diasSemana.split(SEPARADOR_DIAS))
				.map(String::trim)
				.filter(dia -> !dia.isEmpty())
				.toList();
	}

	public final void setDiasSeleccionados(List<String> dias) {
		List<String> normalizados = (dias == null)
				? List.of()
				: dias.stream()
						.filter(Objects::nonNull)
						.map(String::trim)
						.filter(dia -> !dia.isEmpty())
						.toList();

		if (normalizados.isEmpty()) {
			this.diasSemana = String.join(SEPARADOR_DIAS, TODOS_LOS_DIAS);
		} else {
			this.diasSemana = normalizados.stream().collect(Collectors.joining(SEPARADOR_DIAS));
		}
	}

	public boolean isRecordatorioActivo() {
		return recordatorioActivo;
	}

	public void setRecordatorioActivo(boolean recordatorioActivo) {
		this.recordatorioActivo = recordatorioActivo;
	}

	public String getNotas() {
		return notas;
	}

	public void setNotas(String notas) {
		this.notas = notas;
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
