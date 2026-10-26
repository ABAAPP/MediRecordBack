package com.api.medirecord.medicamento;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoint de compatibilidad directa con la aplicacion movil MediRecordFront
 * ({@code POST /api/medications}) para el requerimiento B-06.
 */
@RestController
@RequestMapping("/api/medications")
@Tag(name = "Medicamentos (App Movil)", description = "Endpoints consumidos directamente por la aplicacion movil MediRecordFront")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MobileMedicationController {

	private final MedicamentoService service;

	public MobileMedicationController(MedicamentoService service) {
		this.service = service;
	}

	@Operation(
			summary = "Registra un medicamento desde la aplicacion movil (B-06)",
			description = "Valida los datos enviados por la app, almacena el medicamento en PostgreSQL y lo asocia al usuario autenticado.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Medicamento registrado exitosamente"),
			@ApiResponse(responseCode = "400", description = "Datos del medicamento invalidos"),
			@ApiResponse(responseCode = "401", description = "Credenciales ausentes o invalidas")
	})
	@PostMapping
	public ResponseEntity<MobileMedicationResponse> registerMedication(
			@Valid @RequestBody MobileMedicationRequest request) {
		MobileMedicationResponse response = service.registrarDesdeApp(request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/medicamentos/{id}")
				.buildAndExpand(response.medicationId())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

}
