package com.api.medirecord.paciente;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.api.medirecord.common.error.ErrorCode;
import com.api.medirecord.config.CorsConfig;
import com.api.medirecord.config.CorsProperties;
import com.api.medirecord.config.SecurityConfig;
import com.api.medirecord.paciente.dto.PacienteRequest;
import com.api.medirecord.paciente.dto.PacienteResponse;
import com.api.medirecord.paciente.exception.PacienteNotFoundException;

/**
 * Test de slice del controller: se carga solo la capa web + la configuracion de
 * seguridad real (SecurityConfig), con el servicio mockeado.
 *
 * <p>Verifica ademas el contrato de errores (ProblemDetail) y que la API exige
 * token JWT.</p>
 */
@WebMvcTest(PacienteController.class)
@Import({ SecurityConfig.class, CorsConfig.class, PacienteControllerTest.TestCorsProperties.class })
@AutoConfigureMockMvc
class PacienteControllerTest {

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
	private PacienteService service;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final UUID PACIENTE_ID = UUID.fromString("3f7c8b2e-1d4a-4c9e-9f1b-7a2c3d4e5f60");

	private static PacienteResponse response() {
		return new PacienteResponse(PACIENTE_ID, "Ana", "Gomez", "Lopez", "Ana Gomez Lopez",
				LocalDate.of(1990, 5, 14), 36, "ana.gomez@correo.com", "+52 664 123 4567",
				true, null, null);
	}

	@Test
	@DisplayName("Sin token responde 401 en formato ProblemDetail")
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/api/v1/pacientes"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.getCode()))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	@DisplayName("Con token obtiene el paciente y lo envuelve en ApiResponse")
	void obtenerPaciente() throws Exception {
		given(service.obtener(PACIENTE_ID)).willReturn(response());

		mockMvc.perform(get("/api/v1/pacientes/{id}", PACIENTE_ID).with(user("medico").roles("MEDICO")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(PACIENTE_ID.toString()))
				.andExpect(jsonPath("$.data.nombreCompleto").value("Ana Gomez Lopez"));
	}

	@Test
	@DisplayName("Crea un paciente y devuelve 201 con cabecera Location")
	void crearPaciente() throws Exception {
		given(service.crear(any(PacienteRequest.class))).willReturn(response());

		mockMvc.perform(post("/api/v1/pacientes")
						.with(user("recepcion").roles("RECEPCION"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "Ana",
								  "apellidoPaterno": "Gomez",
								  "apellidoMaterno": "Lopez",
								  "fechaNacimiento": "1990-05-14",
								  "email": "ana.gomez@correo.com",
								  "telefono": "+52 664 123 4567"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/pacientes/" + PACIENTE_ID))
				.andExpect(jsonPath("$.data.id").value(PACIENTE_ID.toString()));
	}

	@Test
	@DisplayName("Datos invalidos devuelven 400 con el detalle de las violaciones")
	void crearPacienteConDatosInvalidos() throws Exception {
		mockMvc.perform(post("/api/v1/pacientes")
						.with(user("recepcion").roles("RECEPCION"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "",
								  "apellidoPaterno": "Gomez",
								  "fechaNacimiento": "2030-01-01",
								  "email": "no-es-correo"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.hasItems("nombre", "fechaNacimiento", "email")));
	}

	@Test
	@DisplayName("Un id inexistente devuelve 404 con el codigo del catalogo de errores")
	void pacienteInexistente() throws Exception {
		UUID id = UUID.randomUUID();
		given(service.obtener(id)).willThrow(new PacienteNotFoundException(id));

		mockMvc.perform(get("/api/v1/pacientes/{id}", id).with(user("medico").roles("MEDICO")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
				.andExpect(jsonPath("$.detail").value("No se encontro el paciente con id " + id));
	}

	@Test
	@DisplayName("El listado respeta paginacion y filtros del cliente")
	void listarPaginado() throws Exception {
		Page<PacienteResponse> page = new PageImpl<>(List.of(response()), org.springframework.data.domain.PageRequest.of(0, 5),
				1);
		given(service.buscar(eq("gomez"), anyBoolean(), any(Pageable.class))).willReturn(
				com.api.medirecord.common.response.PageResponse.from(page, value -> value));

		mockMvc.perform(get("/api/v1/pacientes")
						.with(user("medico").roles("MEDICO"))
						.param("search", "gomez")
						.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].id").value(PACIENTE_ID.toString()))
				.andExpect(jsonPath("$.data.size").value(5))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

}