package com.api.medirecord.usuario.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** Ya existe un usuario registrado con el mismo correo electronico. */
public class UsuarioEmailDuplicadoException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient String email;

	public UsuarioEmailDuplicadoException(String email) {
		super(ErrorCode.DUPLICATED_RESOURCE, "Ya existe un usuario registrado con el correo " + email);
		this.email = email;
		withDetail("email", email);
	}

	public String getEmail() {
		return email;
	}

}
