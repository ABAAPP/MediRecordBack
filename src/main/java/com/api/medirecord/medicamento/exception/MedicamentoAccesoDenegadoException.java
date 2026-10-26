package com.api.medirecord.medicamento.exception;

import java.util.UUID;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** Intento de acceder o modificar un medicamento perteneciente a otro usuario. */
public class MedicamentoAccesoDenegadoException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient UUID id;

	public MedicamentoAccesoDenegadoException(UUID id) {
		super(ErrorCode.FORBIDDEN, "No cuenta con permisos para acceder al medicamento con id " + id);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

}
