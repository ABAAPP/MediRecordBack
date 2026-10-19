package com.api.medirecord.medicamento.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/**
 * La identidad autenticada (claim {@code sub} del JWT) no tiene una cuenta
 * local activa en MediRecord, por lo que no puede administrar medicamentos.
 */
public class CuentaNoVinculadaException extends ApiException {

	private static final long serialVersionUID = 1L;

	public CuentaNoVinculadaException() {
		super(ErrorCode.FORBIDDEN,
				"La cuenta autenticada no esta vinculada a MediRecord o esta desactivada");
	}

}
