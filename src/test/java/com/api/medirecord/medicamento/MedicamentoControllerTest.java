package com.api.medirecord.medicamento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.common.response.PageResponse;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.medicamento.dto.MedicamentoRequest;
import com.api.medirecord.medicamento.dto.MedicamentoResponse;
import com.api.medirecord.medicamento.exception.CuentaNoVinculadaException;
import com.api.medirecord.medicamento.exception.FechasTratamientoInvalidasException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Test de slice del controller: se carga solo la capa web + la configuracion de
 * seguridad real (SecurityConfig), con el servicio mockeado.
 *
 * <p>Verifica ademas el contrato de errores (ProblemDetail), que la API exige
 * token JWT y que los endpoints son personales (sin id de usuario en la URL).</p>
 */
@WebMvcTest(MedicamentoController.class)
@Import({ SecurityConfig.class, CorsConfig.class, MedicamentoControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class MedicamentoControllerTest {

	@TestConfiguration(proxyBeanMethods = false)
	static class TestCorsProperties {

		@Bean
		CorsProperties corsProperties() {
			return new CorsProperties(List.of("http://localhost:3000"),
					List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
					List.of("Authorization", "Content-Type"),
					List.of("Location"),
					true,
					java.time.Duration.ofHours(1));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MedicamentoService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final UUID MEDICAMENTO_ID = UUID.fromString("8f2c1a94-6b3e-4d7a-a1c5-9e0b2f4d6c81");

	private static MedicamentoResponse response() {
		return new MedicamentoResponse(MEDICAMENTO_ID, "Paracetamol", "500 mg", "Cada 8 horas",
				LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), false, true,
				Instant.parse("2026-10-01T10:00:00Z"), null);
	}

	@Test
	@DisplayName("Sin token responde 401 en formato ProblemDetail")
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/api/v1/medicamentos"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@DisplayName("Registra un medicamento y devuelve 201 con cabecera Location")
	void crearMedicamento() throws Exception {
		given(service.crear(any(MedicamentoRequest.class))).willReturn(response());

		mockMvc.perform(post("/api/v1/medicamentos")
						.with(user("usuario"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "Paracetamol",
								  "dosis": "500 mg",
								  "frecuencia": "Cada 8 horas",
								  "fechaInicio": "2026-10-01",
								  "fechaFin": "2026-10-15"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/medicamentos/" + MEDICAMENTO_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.enTratamiento").value(false));
	}

	@Test
	@DisplayName("Datos invalidos devuelven 400 con el detalle de las violaciones")
	void crearMedicamentoConDatosInvalidos() throws Exception {
		mockMvc.perform(post("/api/v1/medicamentos")
						.with(user("usuario"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "",
								  "fechaInicio": null
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.hasItems("nombre", "fechaInicio")));
	}

	@Test
	@DisplayName("La fecha de fin anterior a la de inicio responde 400 con contexto")
	void fechasInconsistentes() throws Exception {
		given(service.crear(any(MedicamentoRequest.class)))
				.willThrow(new FechasTratamientoInvalidasException(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 1)));

		mockMvc.perform(post("/api/v1/medicamentos")
						.with(user("usuario"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "Paracetamol",
								  "fechaInicio": "2026-10-15",
								  "fechaFin": "2026-10-01"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.detail").value("La fecha de finalizacion no puede ser anterior a la fecha de inicio"))
				.andExpect(jsonPath("$.fechaInicio").value("2026-10-15"))
				.andExpect(jsonPath("$.fechaFin").value("2026-10-01"));
	}

	@Test
	@DisplayName("Con token obtiene el medicamento y lo envuelve en ApiResponse")
	void obtenerMedicamento() throws Exception {
		given(service.obtener(MEDICAMENTO_ID)).willReturn(response());

		mockMvc.perform(get("/api/v1/medicamentos/{id}", MEDICAMENTO_ID).with(user("usuario")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.nombre").value("Paracetamol"));
	}

	@Test
	@DisplayName("Un id ajeno o inexistente devuelve 404 sin revelar mas datos")
	void medicamentoInexistente() throws Exception {
		UUID id = UUID.randomUUID();
		given(service.obtener(id)).willThrow(new MedicamentoNotFoundException(id));

		mockMvc.perform(get("/api/v1/medicamentos/{id}", id).with(user("usuario")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.detail").value("No se encontro el medicamento con id " + id));
	}

	@Test
	@DisplayName("Cuenta sin vinculo local responde 403")
	void cuentaNoVinculada() throws Exception {
		given(service.listar(any(), anyBoolean(), any(Pageable.class)))
				.willThrow(new CuentaNoVinculadaException());

		mockMvc.perform(get("/api/v1/medicamentos").with(user("usuario")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));
	}

	@Test
	@DisplayName("El listado respeta paginacion y filtros del cliente")
	void listarPaginado() throws Exception {
		Page<MedicamentoResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 5), 1);
		given(service.listar(eq("para"), anyBoolean(), any(Pageable.class)))
				.willReturn(PageResponse.from(page, value -> value));

		mockMvc.perform(get("/api/v1/medicamentos")
						.with(user("usuario"))
						.param("search", "para")
						.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].id").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.size").value(5))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("Actualiza un medicamento propio con PUT")
	void actualizarMedicamento() throws Exception {
		given(service.actualizar(eq(MEDICAMENTO_ID), any(MedicamentoRequest.class))).willReturn(response());

		mockMvc.perform(put("/api/v1/medicamentos/{id}", MEDICAMENTO_ID)
						.with(user("usuario"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "Paracetamol",
								  "dosis": "500 mg",
								  "frecuencia": "Cada 8 horas",
								  "fechaInicio": "2026-10-01",
								  "fechaFin": "2026-10-15"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(MEDICAMENTO_ID.toString()));
	}

	@Test
	@DisplayName("La baja logica responde 200 con mensaje")
	void desactivarMedicamento() throws Exception {
		mockMvc.perform(patch("/api/v1/medicamentos/{id}/desactivar", MEDICAMENTO_ID).with(user("usuario")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Medicamento dado de baja"));
	}

	@Test
	@DisplayName("DELETE es alias de baja logica")
	void eliminarMedicamento() throws Exception {
		mockMvc.perform(delete("/api/v1/medicamentos/{id}", MEDICAMENTO_ID).with(user("usuario")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Medicamento dado de baja"));
	}

}
