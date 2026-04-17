package com.ejemplo.facturacion.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ejemplo.facturacion.entities.Usuario;
import com.ejemplo.facturacion.repositories.UsuarioRepository;

// Clase para inicializar datos en la base de datos al iniciar la aplicación
@Component
public class DataInitializer implements ApplicationRunner {

    // Repositorio para acceder a los datos de usuarios
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Codificador de contraseñas para asegurar que las contraseñas se almacenen de forma segura
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Método que se ejecuta al iniciar la aplicación para crear un usuario por defecto si no existe
    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByNombreUsuario("developer").isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setId(1);
            usuario.setNombreUsuario("developer");
            usuario.setContraseña(passwordEncoder.encode("12345"));
            usuario.setAutoridad("facturar");
            usuarioRepository.save(usuario);
        }
    }
}
