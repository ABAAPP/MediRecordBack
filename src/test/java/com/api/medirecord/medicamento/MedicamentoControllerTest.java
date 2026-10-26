package com.api.medirecord.medicamento;

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
import com.api.medirecord.medicamento.exception.MedicamentoAccesoDenegadoException;
import com.api.medirecord.medicamento.exception.MedicamentoNotFoundException;

/**
 * Test de slice del controller de medicamentos (B-05).
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

	private static final UUID MEDICAMENTO_ID = UUID.fromString("7a1d2c3b-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
	private static final String USUARIO_ID = "usuario-123";

	private static MedicamentoResponse response() {
		return new MedicamentoResponse(
				MEDICAMENTO_ID,
				USUARIO_ID,
				"Paracetamol",
				"500 mg - 1 tableta",
				"Cada 8 horas",
				"Con alimentos",
				LocalDate.of(2026, 10, 25),
				LocalDate.of(2026, 11, 1),
				true,
				true,
				null,
				null);
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
						.with(user(USUARIO_ID).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "Paracetamol",
								  "dosis": "500 mg - 1 tableta",
								  "frecuencia": "Cada 8 horas",
								  "instrucciones": "Con alimentos",
								  "fechaInicio": "2026-10-25",
								  "fechaFin": "2026-11-01"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/medicamentos/" + MEDICAMENTO_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.usuarioId").value(USUARIO_ID))
				.andExpect(jsonPath("$.data.nombre").value("Paracetamol"))
				.andExpect(jsonPath("$.data.fechaInicio").value("2026-10-25"))
				.andExpect(jsonPath("$.data.fechaFin").value("2026-11-01"));
	}

	@Test
	@DisplayName("Datos invalidos (sin nombre o sin fecha de inicio) devuelven 400")
	void crearMedicamentoConDatosInvalidos() throws Exception {
		mockMvc.perform(post("/api/v1/medicamentos")
						.with(user(USUARIO_ID).roles("MEDICO"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombre": "",
								  "dosis": "500 mg"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
				.andExpect(jsonPath("$.errors[*].field",
						org.hamcrest.Matchers.hasItems("nombre", "fechaInicio")));
	}

	@Test
	@DisplayName("Intentar acceder a un medicamento de otro usuario devuelve 403 FORBIDDEN")
	void accesoDenegadoAMedicamentoDeOtroUsuario() throws Exception {
		UUID idAjeno = UUID.randomUUID();
		given(service.obtener(idAjeno)).willThrow(new MedicamentoAccesoDenegadoException(idAjeno));

		mockMvc.perform(get("/api/v1/medicamentos/{id}", idAjeno)
						.with(user(USUARIO_ID).roles("MEDICO")))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	@DisplayName("Un id inexistente devuelve 404 NOT_FOUND")
	void medicamentoInexistente() throws Exception {
		UUID id = UUID.randomUUID();
		given(service.obtener(id)).willThrow(new MedicamentoNotFoundException(id));

		mockMvc.perform(get("/api/v1/medicamentos/{id}", id)
						.with(user(USUARIO_ID).roles("MEDICO")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));
	}

	@Test
	@DisplayName("Lista paginada de medicamentos del usuario autenticado")
	void listarMedicamentos() throws Exception {
		Page<MedicamentoResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1);
		given(service.buscar(eq("paracetamol"), anyBoolean(), any(Pageable.class)))
				.willReturn(PageResponse.from(page, value -> value));

		mockMvc.perform(get("/api/v1/medicamentos")
						.with(user(USUARIO_ID).roles("MEDICO"))
						.param("search", "paracetamol")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].id").value(MEDICAMENTO_ID.toString()))
				.andExpect(jsonPath("$.data.content[0].nombre").value("Paracetamol"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

}
