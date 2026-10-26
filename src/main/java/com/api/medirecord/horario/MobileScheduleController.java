package com.api.medirecord.horario;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.horario.dto.MobileScheduleRequest;
import com.api.medirecord.horario.dto.MobileScheduleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoint compatible con la aplicacion movil MediRecordFront
 * ({@code POST /api/schedules}) para el requerimiento B-07.
 */
@RestController
@RequestMapping("/api/schedules")
@Tag(name = "Horarios (App Movil)", description = "Configuracion de horarios consumida por la aplicacion movil MediRecordFront")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MobileScheduleController {

	private final HorarioService service;

	public MobileScheduleController(HorarioService service) {
		this.service = service;
	}

	@Operation(summary = "Guarda el horario y frecuencia de un medicamento desde la aplicacion movil (B-07)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Horario configurado exitosamente"),
			@ApiResponse(responseCode = "400", description = "Datos invalidos"),
			@ApiResponse(responseCode = "404", description = "El medicamento asociado no existe")
	})
	@PostMapping
	public ResponseEntity<MobileScheduleResponse> saveMedicationSchedule(
			@Valid @RequestBody MobileScheduleRequest request) {
		MobileScheduleResponse response = service.guardarDesdeApp(request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/horarios/{id}")
				.buildAndExpand(response.scheduleId())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

}
