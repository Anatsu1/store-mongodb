package com.boostmyfool.beastore.controllers;

import com.boostmyfool.beastore.models.Productos;
import com.boostmyfool.beastore.models.ProductosDTO;
import com.boostmyfool.beastore.services.ProductoNoEncontradoException;
import com.boostmyfool.beastore.services.ProductoService;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/productos")
public class ControladorProductos {

    private static final Logger log = LoggerFactory.getLogger(ControladorProductos.class);

    /** Categorias disponibles; se inyectan en los formularios para no repetirlas en las vistas. */
    private static final List<String> CATEGORIAS = List.of(
            "Notebooks", "Telefonos", "PCs", "Accesorios", "Camaras", "Impresoras", "Otros");

    private final ProductoService servicio;

    public ControladorProductos(ProductoService servicio) {
        this.servicio = servicio;
    }

    @ModelAttribute("categorias")
    public List<String> categorias() {
        return CATEGORIAS;
    }

    @GetMapping({"", "/"})
    public String mostrarListaProductos(Model model) {
        model.addAttribute("productos", servicio.listarTodos());
        return "productos/tablaProductos";
    }

    @GetMapping("/crear")
    public String mostrarCreacionProductos(Model model) {
        model.addAttribute("productoDTO", new ProductosDTO());
        return "productos/crearProducto";
    }

    @PostMapping("/crear")
    public String crearProducto(@Valid @ModelAttribute("productoDTO") ProductosDTO productoDTO,
                                BindingResult resultado,
                                RedirectAttributes redirect) throws IOException {

        if (!ProductoService.tieneArchivo(productoDTO.getImagenArchivo())) {
            resultado.addError(new FieldError("productoDTO", "imagenArchivo",
                    "El archivo de imagen es necesario."));
        }
        if (resultado.hasErrors()) {
            return "productos/crearProducto";
        }

        Productos creado = servicio.crear(productoDTO);
        redirect.addFlashAttribute("mensaje", "Producto \"" + creado.getNombre() + "\" creado correctamente.");
        return "redirect:/productos";
    }

    @GetMapping("/edit/{id}")
    public String mostrarModificacionProductos(Model model, @PathVariable ObjectId id) {
        Productos producto = servicio.buscarPorId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("productoDTO", servicio.comoDTO(producto));
        return "productos/editarProducto";
    }

    @PostMapping("/edit/{id}")
    public String actualizarProducto(Model model,
                                     @PathVariable ObjectId id,
                                     @Valid @ModelAttribute("productoDTO") ProductosDTO productoDTO,
                                     BindingResult resultado,
                                     RedirectAttributes redirect) throws IOException {

        Productos producto = servicio.buscarPorId(id);

        // La vista de edicion muestra la imagen y la fecha actuales: el producto
        // debe volver al modelo tambien cuando el formulario se re-renderiza con errores.
        if (resultado.hasErrors()) {
            model.addAttribute("producto", producto);
            return "productos/editarProducto";
        }

        Productos actualizado = servicio.actualizar(id, productoDTO);
        redirect.addFlashAttribute("mensaje", "Producto \"" + actualizado.getNombre() + "\" actualizado.");
        return "redirect:/productos";
    }

    @PostMapping("/delete/{id}")
    public String eliminarProducto(@PathVariable ObjectId id, RedirectAttributes redirect) {
        servicio.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Producto eliminado.");
        return "redirect:/productos";
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public String productoNoEncontrado(ProductoNoEncontradoException e, RedirectAttributes redirect) {
        log.warn("{}", e.getMessage());
        redirect.addFlashAttribute("error", "El producto solicitado ya no existe.");
        return "redirect:/productos";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String imagenDemasiadoGrande(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "La imagen supera el tamano maximo permitido (5 MB).");
        return "redirect:/productos";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String identificadorInvalido(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "El identificador de producto no es valido.");
        return "redirect:/productos";
    }
}
