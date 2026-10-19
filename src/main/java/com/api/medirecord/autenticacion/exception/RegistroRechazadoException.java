package com.api.medirecord.autenticacion.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** El proveedor de identidad rechazo los datos del alta (p. ej. politica de contrasenas). */
public class RegistroRechazadoException extends ApiException {

	private static final long serialVersionUID = 1L;

	public RegistroRechazadoException(String detalle) {
		super(ErrorCode.VALIDATION_ERROR,
				"El servicio de autenticacion rechazo los datos del registro: " + detalle);
		withDetail("proveedor", "keycloak");
	}

}
