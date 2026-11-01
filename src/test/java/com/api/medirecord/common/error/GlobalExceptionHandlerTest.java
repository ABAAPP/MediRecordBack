package com.api.medirecord.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Suite de pruebas unitarias para {@link GlobalExceptionHandler} (Requerimiento B-11).
 *
 * <p>Verifica el cumplimiento de todos los criterios de aceptacion del requerimiento:
 * <ul>
 *   <li>1. Los campos obligatorios son validados (400 VALIDATION_ERROR y lista de errores).</li>
 *   <li>2. Se rechazan datos con formatos invalidos (400 MALFORMED_REQUEST / TYPE_MISMATCH).</li>
 *   <li>3. Se manejan errores de autenticacion (401 UNAUTHENTICATED).</li>
 *   <li>4. Se manejan recursos inexistentes (404 NOT_FOUND).</li>
 *   <li>5. Se manejan errores de acceso no autorizado (403 FORBIDDEN).</li>
 *   <li>6. Las respuestas HTTP utilizan codigos apropiados (400, 401, 403, 404, 405, 409, 500).</li>
 *   <li>7. Los mensajes de error tienen una estructura consistente (RFC 9457 ProblemDetail).</li>
 *   <li>8. Los errores no exponen informacion sensible del sistema (sin SQL, stacktraces ni nombres de paquetes).</li>
 * </ul>
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

	@Test
	@DisplayName("Maneja errores de autenticacion retornando 401 UNAUTHENTICATED")
	void debeManejarErrorDeAutenticacion() {
		var ex = mock(AuthenticationException.class);
		given(ex.getMessage()).willReturn("Full authentication is required to access this resource");

		ProblemDetail detail = handler.handleAuthentication(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(detail.getTitle()).isEqualTo("UNAUTHENTICATED");
		assertThat(detail.getProperties()).containsEntry("code", "UNAUTHENTICATED");
		assertThat(detail.getDetail()).isEqualTo(ErrorCode.UNAUTHENTICATED.getDefaultMessage());
	}

	@Test
	@DisplayName("Maneja acceso denegado por seguridad retornando 403 FORBIDDEN")
	void debeManejarAccesoDenegadoSeguridad() {
		var ex = new AccessDeniedException("Access is denied");

		ProblemDetail detail = handler.handleAccessDenied(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertThat(detail.getTitle()).isEqualTo("FORBIDDEN");
		assertThat(detail.getProperties()).containsEntry("code", "FORBIDDEN");
		assertThat(detail.getDetail()).isEqualTo(ErrorCode.FORBIDDEN.getDefaultMessage());
	}

	@Test
	@DisplayName("Maneja acceso denegado a recurso ajeno (MedicamentoAccesoDenegadoException) retornando 403 FORBIDDEN")
	void debeManejarAccesoDenegadoRecursoAjeno() {
		UUID id = UUID.randomUUID();
		var ex = new MedicamentoAccesoDenegadoException(id);

		ProblemDetail detail = handler.handleApiException(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertThat(detail.getTitle()).isEqualTo("FORBIDDEN");
		assertThat(detail.getProperties()).containsEntry("code", "FORBIDDEN");
		assertThat(detail.getDetail()).contains(id.toString());
	}

	@Test
	@DisplayName("Maneja recurso inexistente de negocio (MedicamentoNotFoundException) retornando 404 NOT_FOUND")
	void debeManejarMedicamentoNotFoundException() {
		UUID id = UUID.randomUUID();
		var ex = new MedicamentoNotFoundException(id);

		ProblemDetail detail = handler.handleApiException(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(detail.getTitle()).isEqualTo("NOT_FOUND");
		assertThat(detail.getProperties()).containsEntry("code", "NOT_FOUND");
		assertThat(detail.getDetail()).contains(id.toString());
	}

	@Test
	@DisplayName("Maneja ruta web inexistente (NoResourceFoundException) retornando 404 NOT_FOUND")
	void debeManejarRutaInexistente() {
		var ex = mock(NoResourceFoundException.class);
		given(ex.getResourcePath()).willReturn("/api/v1/no-existe");

		ProblemDetail detail = handler.handleNoResourceFound(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(detail.getTitle()).isEqualTo("NOT_FOUND");
		assertThat(detail.getProperties()).containsEntry("code", "NOT_FOUND");
		assertThat(detail.getDetail()).contains("/api/v1/no-existe");
	}

	@Test
	@DisplayName("Maneja metodo HTTP no soportado retornando 405 METHOD_NOT_ALLOWED")
	void debeManejarMetodoNoSoportado() {
		var ex = new HttpRequestMethodNotSupportedException("PATCH");

		ProblemDetail detail = handler.handleMethodNotSupported(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
		assertThat(detail.getTitle()).isEqualTo("METHOD_NOT_ALLOWED");
		assertThat(detail.getProperties()).containsEntry("code", "METHOD_NOT_ALLOWED");
		assertThat(detail.getDetail()).contains("PATCH");
	}

	@Test
	@DisplayName("Maneja violacion de integridad en base de datos retornando 409 DATA_INTEGRITY_ERROR sin filtrar SQL sensible")
	void debeManejarViolacionDeIntegridadSinFiltrarSql() {
		var cause = new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_pacientes_email\"\nDetail: Key (email)=(test@example.com) already exists.");
		var ex = new DataIntegrityViolationException("Error de BD", cause);

		ProblemDetail detail = handler.handleDataIntegrityViolation(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(detail.getTitle()).isEqualTo("DATA_INTEGRITY_ERROR");
		assertThat(detail.getProperties()).containsEntry("code", "DATA_INTEGRITY_ERROR");
		assertThat(detail.getDetail()).isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR.getDefaultMessage());
		assertThat(detail.getDetail()).doesNotContain("uk_pacientes_email");
		assertThat(detail.getDetail()).doesNotContain("duplicate key value");
	}

	@Test
	@DisplayName("Maneja fallo por concurrencia optimista retornando 409 CONCURRENT_MODIFICATION")
	void debeManejarFalloConcurrenciaOptimista() {
		var ex = new OptimisticLockingFailureException("Conflicto de version de entidad");

		ProblemDetail detail = handler.handleOptimisticLocking(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(detail.getTitle()).isEqualTo("CONCURRENT_MODIFICATION");
		assertThat(detail.getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
	}

	@Test
	@DisplayName("Maneja error inesperado no controlado retornando 500 INTERNAL_ERROR sin exponer stack trace")
	void debeManejarErrorInesperadoSinExponerStackTrace() {
		var ex = new NullPointerException("Null reference at internal secret algorithm");

		ProblemDetail detail = handler.handleUnexpected(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(detail.getTitle()).isEqualTo("INTERNAL_ERROR");
		assertThat(detail.getProperties()).containsEntry("code", "INTERNAL_ERROR");
		assertThat(detail.getDetail()).isEqualTo("Error interno inesperado");
		assertThat(detail.getDetail()).doesNotContain("Null reference");
		assertThat(detail.getDetail()).doesNotContain("secret algorithm");
	}

	@Test
	@DisplayName("Garantiza la estructura consistente de respuesta RFC 9457 ProblemDetail")
	void debeTenerEstructuraConsistenteProblemDetail() {
		var ex = new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "Servicio temporalmente saturado");

		ProblemDetail detail = handler.handleApiException(ex, request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
		assertThat(detail.getTitle()).isEqualTo("SERVICE_UNAVAILABLE");
		assertThat(detail.getDetail()).isEqualTo("Servicio temporalmente saturado");
		assertThat(detail.getInstance()).isEqualTo(URI.create("/api/v1/medicamentos"));
		assertThat(detail.getProperties()).containsKey("code");
		assertThat(detail.getProperties()).containsKey("timestamp");
	}
}
