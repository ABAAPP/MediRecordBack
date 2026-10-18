package com.api.medirecord.autenticacion.dto;

import com.api.medirecord.usuario.dto.UsuarioResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado de un inicio de sesion exitoso: tokens de sesion y datos de la cuenta. */
@Schema(name = "LoginResponse", description = "Sesion iniciada: tokens y cuenta autenticada")
public record LoginResponse(

		@Schema(description = "Tokens de la sesion") TokenResponse tokens,

		@Schema(description = "Cuenta autenticada") UsuarioResponse usuario) {
}
