package com.boostmyfool.beastore.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** Datos que llegan desde el formulario, con sus reglas de validacion. */
public class ProductosDTO {

    @NotEmpty(message = "El nombre es requerido")
    private String nombre;

    @NotEmpty(message = "La marca es requerida")
    private String marca;

    @NotEmpty(message = "La categoria es requerida")
    private String categoria;

    @Min(value = 0, message = "El precio no puede ser negativo")
    private double precio;

    @Size(min = 10, message = "La descripcion debe contener por lo menos 10 caracteres")
    @Size(max = 2000, message = "La descripcion no debe superar los 2000 caracteres")
    private String descripcion;

    private MultipartFile imagenArchivo;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public MultipartFile getImagenArchivo() {
        return imagenArchivo;
    }

    public void setImagenArchivo(MultipartFile imagenArchivo) {
        this.imagenArchivo = imagenArchivo;
    }
}
