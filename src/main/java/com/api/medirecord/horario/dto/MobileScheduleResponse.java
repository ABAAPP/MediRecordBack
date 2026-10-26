package com.api.medirecord.horario.dto;

import java.util.List;

import com.api.medirecord.horario.Horario;
import com.api.medirecord.medicamento.Medicamento;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta devuelta a la aplicacion movil MediRecordFront al configurar un
 * horario ({@code POST /api/schedules}).
 */
@Schema(name = "MobileScheduleResponse", description = "Respuesta de configuracion de horario para la app movil")
public record MobileScheduleResponse(
		boolean success,
		String message,
		String scheduleId,
		MobileMedicationDoseDto updatedMedication) {

	public static MobileScheduleResponse from(Horario horario) {
		Medicamento med = horario.getMedicamento();
		List<String> dias = horario.getDiasSeleccionados();
		String instrucciones = (horario.getNotas() != null && !horario.getNotas().isBlank())
				? horario.getNotas()
				: (med != null ? med.getInstrucciones() : null);

		MobileMedicationDoseDto doseDto = med == null ? null : new MobileMedicationDoseDto(
				med.getId() != null ? med.getId().toString() : null,
				med.getNombre(),
				med.getDosis() != null ? med.getDosis() : "",
				horario.getHoraAdministracion(),
				false,
				instrucciones,
				horario.getFrecuencia(),
				dias,
				med.getUsuarioId(),
				null,
				med.getFechaInicio() != null ? med.getFechaInicio().toString() : null);

		String nombreMed = med != null ? med.getNombre() : "";
		return new MobileScheduleResponse(
				true,
				"Horario configurado correctamente para '" + nombreMed + "' a las " + horario.getHoraAdministracion(),
				horario.getId() != null ? horario.getId().toString() : null,
				doseDto);
	}

}
