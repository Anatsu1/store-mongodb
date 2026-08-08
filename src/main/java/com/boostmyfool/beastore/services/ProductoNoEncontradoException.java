package com.boostmyfool.beastore.services;

import org.bson.types.ObjectId;

/** Se lanza cuando se pide un producto cuyo identificador no existe en la coleccion. */
public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(ObjectId id) {
        super("No existe un producto con el id " + id);
    }
}
