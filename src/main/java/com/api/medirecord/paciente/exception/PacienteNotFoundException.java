package com.api.medirecord.paciente.exception;

import java.util.UUID;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** El paciente solicitado no existe. */
public class PacienteNotFoundException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient UUID id;

	public PacienteNotFoundException(UUID id) {
		super(ErrorCode.NOT_FOUND, "No se encontro el paciente con id " + id);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

}