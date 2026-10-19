package com.api.medirecord.autenticacion.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/**
 * El proveedor de identidad no respondio o devolvio un error no atribuible al
 * usuario (caida, error 5xx, credenciales de administracion invalidas).
 *
 * <p>Se traduce a {@code 503} para que el cliente reintente mas tarde sin exponer
 * detalles internos del proveedor.</p>
 */
public class KeycloakNoDisponibleException extends ApiException {

	private static final long serialVersionUID = 1L;

	public KeycloakNoDisponibleException(Throwable cause) {
		super(ErrorCode.SERVICE_UNAVAILABLE,
				"El servicio de autenticacion no esta disponible temporalmente", cause);
	}

}
