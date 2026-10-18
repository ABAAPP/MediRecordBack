package com.api.medirecord.autenticacion.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** La cuenta existe y las credenciales son validas, pero esta dada de baja (activo = false). */
public class CuentaDesactivadaException extends ApiException {

	private static final long serialVersionUID = 1L;

	public CuentaDesactivadaException() {
		super(ErrorCode.FORBIDDEN, "La cuenta esta desactivada; contacte al administrador");
	}

}
