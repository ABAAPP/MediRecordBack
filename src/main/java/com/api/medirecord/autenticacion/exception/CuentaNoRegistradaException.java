package com.api.medirecord.autenticacion.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/**
 * La identidad autenticada en el proveedor de identidad no tiene una cuenta
 * local asociada en MediRecord.
 *
 * <p> Solo se lanza despues de verificar las credenciales con exito, por lo que
 * no habilita la enumeracion de cuentas.</p>
 */
public class CuentaNoRegistradaException extends ApiException {

	private static final long serialVersionUID = 1L;

	public CuentaNoRegistradaException() {
		super(ErrorCode.UNAUTHENTICATED,
				"La cuenta no esta vinculada a MediRecord; registrese para iniciar sesion");
	}

}
