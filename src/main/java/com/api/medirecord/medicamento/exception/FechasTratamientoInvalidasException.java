package com.api.medirecord.medicamento.exception;

import java.time.LocalDate;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/**
 * La fecha de finalizacion del tratamiento es anterior a la fecha de inicio.
 */
public class FechasTratamientoInvalidasException extends ApiException {

	private static final long serialVersionUID = 1L;

	public FechasTratamientoInvalidasException(LocalDate fechaInicio, LocalDate fechaFin) {
		super(ErrorCode.VALIDATION_ERROR,
				"La fecha de finalizacion no puede ser anterior a la fecha de inicio");
		withDetail("fechaInicio", fechaInicio);
		withDetail("fechaFin", fechaFin);
	}

}
