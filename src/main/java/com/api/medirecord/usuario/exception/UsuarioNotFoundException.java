package com.api.medirecord.usuario.exception;

import java.util.UUID;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** El usuario solicitado no existe. */
public class UsuarioNotFoundException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient UUID id;

	public UsuarioNotFoundException(UUID id) {
		super(ErrorCode.NOT_FOUND, "No se encontro el usuario con id " + id);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

}
