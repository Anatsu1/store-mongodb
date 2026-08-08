package com.boostmyfool.beastore.services;

import com.boostmyfool.beastore.models.Productos;
import com.boostmyfool.beastore.models.ProductosDTO;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.boostmyfool.beastore.repositories.ProductosRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

/**
 * Reglas de negocio del catalogo: persistencia en MongoDB y gestion del
 * archivo de imagen asociado a cada producto.
 */
@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductosRepository repo;
    private final Path directorioImagenes;

    public ProductoService(ProductosRepository repo, @Value("${app.upload.dir}") String directorioSubidas) {
        this.repo = repo;
        this.directorioImagenes = Paths.get(directorioSubidas).toAbsolutePath().normalize();
    }

    /** Productos ordenados del mas reciente al mas antiguo. */
    public List<Productos> listarTodos() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Productos buscarPorId(ObjectId id) {
        return repo.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    public long contar() {
        return repo.count();
    }

    public Productos crear(ProductosDTO dto) throws IOException {
        Productos producto = new Productos();
        copiarDatos(dto, producto);
        producto.setFechaCreado(new Date());
        producto.setImagenArchivo(guardarImagen(dto.getImagenArchivo()));
        return repo.save(producto);
    }

    /**
     * Actualiza el producto. La imagen solo se reemplaza si se envio un archivo
     * nuevo; en caso contrario se conserva la existente.
     */
    public Productos actualizar(ObjectId id, ProductosDTO dto) throws IOException {
        Productos producto = buscarPorId(id);
        copiarDatos(dto, producto);

        if (tieneArchivo(dto.getImagenArchivo())) {
            String imagenAnterior = producto.getImagenArchivo();
            producto.setImagenArchivo(guardarImagen(dto.getImagenArchivo()));
            borrarImagen(imagenAnterior);
        }
        return repo.save(producto);
    }

    public void eliminar(ObjectId id) {
        Productos producto = buscarPorId(id);
        repo.delete(producto);
        borrarImagen(producto.getImagenArchivo());
    }

    /** Rellena el DTO con los datos de un producto existente (para el formulario de edicion). */
    public ProductosDTO comoDTO(Productos producto) {
        ProductosDTO dto = new ProductosDTO();
        dto.setNombre(producto.getNombre());
        dto.setMarca(producto.getMarca());
        dto.setCategoria(producto.getCategoria());
        dto.setPrecio(producto.getPrecio());
        dto.setDescripcion(producto.getDescripcion());
        return dto;
    }

    public static boolean tieneArchivo(MultipartFile archivo) {
        return archivo != null && !archivo.isEmpty();
    }

    private void copiarDatos(ProductosDTO dto, Productos producto) {
        producto.setNombre(dto.getNombre());
        producto.setMarca(dto.getMarca());
        producto.setCategoria(dto.getCategoria());
        producto.setPrecio(dto.getPrecio());
        producto.setDescripcion(dto.getDescripcion());
    }

    /**
     * Guarda la imagen con un nombre unico y devuelve ese nombre.
     * El nombre original se sanea para evitar que un ".." en la ruta escriba
     * fuera del directorio de subidas.
     */
    private String guardarImagen(MultipartFile imagen) throws IOException {
        String nombreOriginal = StringUtils.getFilename(StringUtils.cleanPath(
                imagen.getOriginalFilename() == null ? "imagen" : imagen.getOriginalFilename()));

        if (nombreOriginal == null || nombreOriginal.isBlank() || nombreOriginal.contains("..")) {
            nombreOriginal = "imagen";
        }

        String nombreGuardado = System.currentTimeMillis() + "_" + nombreOriginal;
        Files.createDirectories(directorioImagenes);
        Path destino = directorioImagenes.resolve(nombreGuardado).normalize();

        if (!destino.startsWith(directorioImagenes)) {
            throw new IOException("Ruta de destino invalida para la imagen: " + nombreGuardado);
        }

        try (InputStream entrada = imagen.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return nombreGuardado;
    }

    /**
     * Borra la imagen del disco. Un fallo aqui no debe tumbar la operacion:
     * el documento ya se guardo o se elimino, un archivo huerfano solo se registra.
     */
    private void borrarImagen(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return;
        }
        Path ruta = directorioImagenes.resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(directorioImagenes)) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warn("No se pudo borrar la imagen {}: {}", nombreArchivo, e.getMessage());
        }
    }
}
