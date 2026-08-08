/*
 * Arma la etiqueta de estante en vivo mientras se carga el formulario,
 * para ver el producto tal como aparecera en el listado antes de guardarlo.
 */
(function () {
    "use strict";

    const etiqueta = document.querySelector("[data-etiqueta]");
    if (!etiqueta) {
        return;
    }

    const destinos = {
        nombre: etiqueta.querySelector("[data-etiqueta-nombre]"),
        marca: etiqueta.querySelector("[data-etiqueta-marca]"),
        categoria: etiqueta.querySelector("[data-etiqueta-categoria]"),
        precio: etiqueta.querySelector("[data-etiqueta-precio]"),
        foto: etiqueta.querySelector("[data-etiqueta-foto]")
    };

    const moneda = new Intl.NumberFormat("es-AR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function texto(destino, valor, reemplazo) {
        if (destino) {
            destino.textContent = valor && valor.trim() !== "" ? valor : reemplazo;
        }
    }

    function refrescar() {
        const campo = (nombre) => document.querySelector('[name="' + nombre + '"]');

        texto(destinos.nombre, campo("nombre") ? campo("nombre").value : "", "Producto sin nombre");
        texto(destinos.marca, campo("marca") ? campo("marca").value : "", "Marca sin definir");
        texto(destinos.categoria, campo("categoria") ? campo("categoria").value : "", "Sin categoria");

        if (destinos.precio) {
            const crudo = campo("precio") ? parseFloat(campo("precio").value) : NaN;
            destinos.precio.textContent = isNaN(crudo) ? "0,00" : moneda.format(crudo);
        }
    }

    ["nombre", "marca", "categoria", "precio"].forEach(function (nombre) {
        const campo = document.querySelector('[name="' + nombre + '"]');
        if (campo) {
            campo.addEventListener("input", refrescar);
            campo.addEventListener("change", refrescar);
        }
    });

    const archivo = document.querySelector('[name="imagenArchivo"]');
    if (archivo && destinos.foto) {
        archivo.addEventListener("change", function () {
            const elegido = archivo.files && archivo.files[0];
            if (!elegido) {
                return;
            }
            const lector = new FileReader();
            lector.onload = function (evento) {
                destinos.foto.src = evento.target.result;
            };
            lector.readAsDataURL(elegido);
        });
    }

    refrescar();
})();
