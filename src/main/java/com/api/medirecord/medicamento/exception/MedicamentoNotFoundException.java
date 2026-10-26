package com.api.medirecord.medicamento.exception;

import java.util.UUID;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** El medicamento solicitado no existe. */
public class MedicamentoNotFoundException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient UUID id;

	public MedicamentoNotFoundException(UUID id) {
		super(ErrorCode.NOT_FOUND, "No se encontro el medicamento con id " + id);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

}
