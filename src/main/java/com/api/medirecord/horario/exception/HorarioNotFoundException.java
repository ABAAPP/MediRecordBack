package com.api.medirecord.horario.exception;

import java.util.UUID;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** El horario solicitado no existe. */
public class HorarioNotFoundException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient UUID id;

	public HorarioNotFoundException(UUID id) {
		super(ErrorCode.NOT_FOUND, "No se encontro el horario con id " + id);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

}
