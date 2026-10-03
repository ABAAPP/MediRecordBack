package com.api.medirecord.paciente.exception;

import com.api.medirecord.common.error.ApiException;
import com.api.medirecord.common.error.ErrorCode;

/** Ya existe un paciente registrado con el mismo correo electronico. */
public class PacienteEmailDuplicadoException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final transient String email;

	public PacienteEmailDuplicadoException(String email) {
		super(ErrorCode.DUPLICATED_RESOURCE, "Ya existe un paciente registrado con el correo " + email);
		this.email = email;
		withDetail("email", email);
	}

	public String getEmail() {
		return email;
	}

}
