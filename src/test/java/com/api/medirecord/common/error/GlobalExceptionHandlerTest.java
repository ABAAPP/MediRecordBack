package com.api.medirecord.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Suite de pruebas unitarias para validaciones en {@link GlobalExceptionHandler} (B-11).
 */
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
		request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/medicamentos");
	}

	@Test
	@DisplayName("Valida campos obligatorios rechazados retornando 400 y lista de violaciones")
	void debeValidarCamposObligatorios() throws Exception {
		var target = new Object();
		var bindingResult = new BeanPropertyBindingResult(target, "medicamentoRequest");
		bindingResult.addError(new FieldError("medicamentoRequest", "nombre", "", false, null, null, "El nombre es obligatorio"));
		bindingResult.addError(new FieldError("medicamentoRequest", "dosis", null, false, null, null, "La dosis es obligatoria"));

		var method = Object.class.getMethod("toString");
		var methodParameter = new MethodParameter(method, -1);
		var ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

		ProblemDetail detail = handler.handleMethodArgumentNotValid(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("VALIDATION_ERROR");
		assertThat(detail.getProperties()).containsEntry("code", "VALIDATION_ERROR");
		assertThat(detail.getProperties().get("timestamp")).isNotNull();
		assertThat(detail.getInstance()).isEqualTo(URI.create("/api/v1/medicamentos"));

		@SuppressWarnings("unchecked")
		var errors = (List<GlobalExceptionHandler.FieldViolation>) detail.getProperties().get("errors");
		assertThat(errors).hasSize(2);
		assertThat(errors).extracting(GlobalExceptionHandler.FieldViolation::field)
				.containsExactlyInAnyOrder("nombre", "dosis");
	}

	@Test
	@DisplayName("Rechaza datos con formato JSON malformado sin exponer detalles internos (400)")
	void debeRechazarJsonMalformadoSinExponerInternos() {
		var cause = new IllegalArgumentException("JSON parse error: Cannot deserialize value of type java.time.LocalDate");
		var ex = new HttpMessageNotReadableException("JSON parse error", cause, new MockHttpInputMessage(new byte[0]));

		ProblemDetail detail = handler.handleNotReadable(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("MALFORMED_REQUEST");
		assertThat(detail.getProperties()).containsEntry("code", "MALFORMED_REQUEST");
		assertThat(detail.getDetail()).doesNotContain("java.time.LocalDate");
		assertThat(detail.getDetail()).doesNotContain("com.api");
		assertThat(detail.getDetail()).contains("formato");
	}

	@Test
	@DisplayName("Rechaza desajustes de tipo en parametros retornando 400 TYPE_MISMATCH")
	void debeRechazarDesajusteDeTipoEnParametros() {
		var ex = mock(MethodArgumentTypeMismatchException.class);
		given(ex.getName()).willReturn("fecha");
		given(ex.getRequiredType()).willAnswer(inv -> java.time.LocalDate.class);

		ProblemDetail detail = handler.handleTypeMismatch(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("TYPE_MISMATCH");
		assertThat(detail.getProperties()).containsEntry("code", "TYPE_MISMATCH");
		assertThat(detail.getDetail()).contains("fecha");
		assertThat(detail.getDetail()).contains("LocalDate");
	}

	@Test
	@DisplayName("Rechaza parametros requeridos faltantes retornando 400 MISSING_PARAMETER")
	void debeRechazarParametroRequeridoFaltante() {
		var ex = new MissingServletRequestParameterException("fecha", "String");

		ProblemDetail detail = handler.handleMissingParameter(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("MISSING_PARAMETER");
		assertThat(detail.getProperties()).containsEntry("code", "MISSING_PARAMETER");
		assertThat(detail.getDetail()).contains("fecha");
	}

	@Test
	@DisplayName("Maneja ConstraintViolationException con lista de violaciones")
	void debeManejarConstraintViolationException() {
		@SuppressWarnings("unchecked")
		ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
		Path path = mock(Path.class);
		given(path.toString()).willReturn("diasSemana");
		given(violation.getPropertyPath()).willReturn(path);
		given(violation.getMessage()).willReturn("no puede estar vacio");
		given(violation.getInvalidValue()).willReturn(List.of());

		var ex = new ConstraintViolationException("Violacion", java.util.Set.of(violation));

		ProblemDetail detail = handler.handleConstraintViolation(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("VALIDATION_ERROR");
		assertThat(detail.getProperties().get("errors")).isNotNull();
	}
}
