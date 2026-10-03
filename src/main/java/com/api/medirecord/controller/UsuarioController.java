package com.api.medirecord.controller;

import com.api.medirecord.model.Usuario;
import com.api.medirecord.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Endpoints de gestion y consulta de usuarios para recordatorios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios registrados")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Registrar o guardar un nuevo usuario")
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario usuario) {
        Usuario guardado = usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}
