package com.api.medirecord.medicamento.exception;

import java.time.LocalDate;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** La fecha de finalizacion es anterior a la fecha de inicio del medicamento. */
public class MedicamentoFechasInvalidasException extends ApiException {

	private static final long serialVersionUID = 1L;

	public MedicamentoFechasInvalidasException(LocalDate fechaInicio, LocalDate fechaFin) {
		super(ErrorCode.VALIDATION_ERROR,
				"La fecha de finalizacion (" + fechaFin + ") no puede ser anterior a la fecha de inicio (" + fechaInicio + ")");
		withDetail("fechaInicio", fechaInicio);
		withDetail("fechaFin", fechaFin);
	}

}
