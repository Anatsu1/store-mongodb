package com.boostmyfool.beastore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expone como recursos estaticos las imagenes subidas por el usuario.
 * <p>
 * Sin esto, las imagenes guardadas en el sistema de archivos ({@code public/images})
 * no son alcanzables desde el navegador: Spring Boot solo sirve estaticos desde el
 * classpath, por lo que todos los {@code <img src="/images/...">} respondian 404.
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final Path directorioImagenes;

    public ConfiguracionWeb(@Value("${app.upload.dir}") String directorioSubidas) {
        this.directorioImagenes = Paths.get(directorioSubidas).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/images/**")
                .addResourceLocations(directorioImagenes.toUri().toString());
    }
}
