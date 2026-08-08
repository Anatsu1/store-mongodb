/* Pide confirmacion antes de eliminar, nombrando el producto en cuestion. */
(function () {
    "use strict";

    document.querySelectorAll("form[data-confirmar]").forEach(function (formulario) {
        formulario.addEventListener("submit", function (evento) {
            const nombre = formulario.getAttribute("data-confirmar") || "este producto";
            if (!window.confirm("¿Eliminar " + nombre + "? Esta acción no se puede deshacer.")) {
                evento.preventDefault();
            }
        });
    });
})();
