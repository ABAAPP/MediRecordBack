package com.api.medirecord.medicamento;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.api.medirecord.config.OpenApiConfig;
import com.api.medirecord.medicamento.dto.MobileMedicationDoseDto;
import com.api.medirecord.medicamento.dto.MobileMedicationRequest;
import com.api.medirecord.medicamento.dto.MobileMedicationResponse;
import com.api.medirecord.toma.TomaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints consumidos directamente por la aplicacion movil MediRecordFront
 * bajo {@code /api/medications/**} (B-06, B-09 y B-10).
 */
@RestController
@RequestMapping("/api/medications")
@Tag(name = "Medicamentos (App Movil)", description = "Endpoints consumidos directamente por la aplicacion movil MediRecordFront")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class MobileMedicationController {

	private static final DateTimeFormatter FORMATO_DIA_MES_ANIO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final MedicamentoService service;
	private final TomaService tomaService;

	public MobileMedicationController(MedicamentoService service, TomaService tomaService) {
		this.service = service;
		this.tomaService = tomaService;
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

	@Operation(
			summary = "Consulta los medicamentos programados para el dia en la app movil (B-10)",
			description = "Devuelve las tomas de medicamentos asociadas a sus horarios para la fecha indicada, reflejando si estan pendientes o realizadas.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de medicamentos obtenida exitosamente"),
			@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping("/today")
	public List<MobileMedicationDoseDto> getTodayMedications(
			@RequestParam(required = false) String date,
			@RequestParam(required = false) String userEmail) {
		LocalDate fecha = parsearFechaOpcional(date);
		return service.consultarDosisHoyMovil(fecha, userEmail);
	}

	@Operation(
			summary = "Alterna el estado de una toma (realizada <-> pendiente) en la app movil (B-09 y B-10)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Estado de toma actualizado"),
			@ApiResponse(responseCode = "401", description = "No autenticado"),
			@ApiResponse(responseCode = "403", description = "No tiene permiso para modificar medicamentos de otro usuario")
	})
	@PostMapping("/{id}/toggle")
	public MobileMedicationDoseDto toggleDoseStatus(
			@PathVariable("id") String doseId) {
		return tomaService.alternarEstadoToma(doseId);
	}

	private static LocalDate parsearFechaOpcional(String valor) {
		if (valor == null || valor.isBlank()) {
			return LocalDate.now();
		}
		String limpio = valor.trim();
		try {
			if (limpio.contains("/")) {
				return LocalDate.parse(limpio, FORMATO_DIA_MES_ANIO);
			}
			return LocalDate.parse(limpio);
		} catch (DateTimeParseException ex) {
			return LocalDate.now();
		}
	}

}
