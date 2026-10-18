package com.api.medirecord.autenticacion.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/**
 * El proveedor de identidad rechazo las credenciales (correo o contrasena).
 *
 * <p>El mensaje es deliberadamente generico y identico para "correo inexistente" y
 * "contrasena incorrecta" para no permitir la enumeracion de cuentas.</p>
 */
public class CredencialesInvalidasException extends ApiException {

	private static final long serialVersionUID = 1L;

	public CredencialesInvalidasException() {
		super(ErrorCode.UNAUTHENTICATED, "Correo o contrasena invalidos");
	}

}
