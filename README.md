# BeaStore — Control de catálogo

Panel de administración de un catálogo de productos electrónicos. Permite dar de alta un
producto con su foto, listarlo, corregirlo y darlo de baja. Los datos viven en **MongoDB** y
las imágenes en el sistema de archivos del servidor.

Aplicación web con renderizado en el servidor: **Java 21 · Spring Boot 3.3 · Spring Data MongoDB ·
Thymeleaf**, sin frameworks de CSS ni de JavaScript.

![Portada](docs/capturas/01-portada.png)

---

## Índice

- [Características](#características)
- [Stack](#stack)
- [Arquitectura](#arquitectura)
- [Puesta en marcha](#puesta-en-marcha)
- [Configuración](#configuración)
- [Rutas](#rutas)
- [Modelo de datos](#modelo-de-datos)
- [Validaciones](#validaciones)
- [Capturas](#capturas)
- [Correcciones aplicadas](#correcciones-aplicadas)
- [Próximos pasos](#próximos-pasos)

---

## Características

**CRUD completo sobre MongoDB.** Alta, listado, edición y baja de productos contra una colección
`productos`, usando `MongoRepository` con `ObjectId` como identificador. El listado se ordena del
más reciente al más antiguo.

**Subida de imágenes con el archivo saneado.** Cada foto se guarda en disco con un nombre
compuesto por marca de tiempo y nombre original limpio. El nombre se normaliza y la ruta de
destino se verifica contra el directorio de subidas, de modo que un `../` en el nombre no puede
escribir fuera de él.

**Ciclo de vida de la imagen atado al del producto.** Al reemplazar la foto se borra la anterior;
al eliminar el producto se borra su archivo. Si el borrado del archivo falla, la operación no se
interrumpe: queda registrado en el log.

**Validación en servidor con mensajes por campo.** Bean Validation sobre el DTO del formulario.
Cuando algo no pasa, el formulario se vuelve a mostrar con los valores cargados y el error debajo
del campo que lo causó.

**Etiqueta de estante en vivo.** Mientras se completa el formulario, un panel lateral arma la
etiqueta del producto —foto, categoría, nombre, marca y precio— tal como se verá en el listado,
antes de guardar nada.

**Avisos de resultado.** Cada alta, edición o baja redirige al listado con un mensaje que confirma
lo que pasó. Un identificador inexistente o mal formado no rompe: vuelve al listado con un aviso.

**Interfaz propia, sin framework de CSS.** Hoja de estilos escrita a mano (~790 líneas), adaptada
a teléfono, con foco de teclado visible y respeto por `prefers-reduced-motion`.

---

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.4 (Web, Validation, DevTools) |
| Persistencia | Spring Data MongoDB · driver 5.0 |
| Vistas | Thymeleaf 3 con fragmentos |
| Estilos | CSS propio, sin dependencias |
| Interacción | JavaScript sin librerías |
| Build | Maven (con wrapper `mvnw`) |

Tipografías servidas desde Google Fonts: Archivo (titulares), Instrument Sans (texto),
IBM Plex Mono (datos y precios).

---

## Arquitectura

Tres capas, con el controlador reducido a coordinar HTTP y la lógica concentrada en el servicio.

```
Navegador
   │
   ▼
ControladorProductos ──── rutas, binding del formulario, validación, avisos
   │
   ▼
ProductoService ───────── reglas de negocio, guardado y borrado de imágenes
   │
   ▼
ProductosRepository ───── MongoRepository<Productos, ObjectId>
   │
   ▼
MongoDB (colección "productos")   +   public/images/ (archivos)
```

```
src/main/
├── java/com/boostmyfool/beastore/
│   ├── BeastoreApplication.java
│   ├── config/
│   │   └── ConfiguracionWeb.java              Publica public/images en la ruta /images/**
│   ├── controllers/
│   │   └── ControladorProductos.java          Rutas, validación y manejo de errores
│   ├── models/
│   │   ├── Productos.java                     Documento de MongoDB
│   │   └── ProductosDTO.java                  Datos del formulario y sus restricciones
│   ├── repositories/
│   │   └── ProductosRepository.java
│   └── services/
│       ├── ProductoService.java               Reglas de negocio y archivos
│       └── ProductoNoEncontradoException.java
└── resources/
    ├── application.properties
    ├── static/
    │   ├── index.html                         Portada
    │   ├── css/beastore.css                   Sistema visual completo
    │   └── js/{etiqueta,catalogo}.js
    └── templates/
        ├── fragments/base.html                Cabecera, barra y avisos compartidos
        └── productos/{tablaProductos,crearProducto,editarProducto}.html
```

---

## Puesta en marcha

### Requisitos

- **JDK 21 o superior** (con `javac`; en Fedora es el paquete `java-21-openjdk-devel`)
- **MongoDB 6 o superior** corriendo en `localhost:27017`
- No hace falta instalar Maven: el repositorio incluye el wrapper

### 1. MongoDB

Con el servicio del sistema:

```bash
sudo systemctl start mongod
```

O con un contenedor:

```bash
podman run -d --name mongo-beastore -p 27017:27017 mongo:7   # o docker run …
```

### 2. Arrancar la aplicación

```bash
git clone <url-del-repositorio>
cd store-mongodb
./mvnw spring-boot:run
```

Queda disponible en **http://localhost:8080**.

### 3. Empaquetar

```bash
./mvnw package
java -jar target/beastore-0.0.1-SNAPSHOT.jar
```

> Las imágenes se guardan en `public/images/`, una ruta **relativa al directorio desde el que se
> ejecuta la aplicación**. Si se corre el `.jar` desde otra carpeta, conviene fijar `UPLOAD_DIR`
> con una ruta absoluta.

---

## Configuración

Todo se resuelve con valores por defecto; las dos variables de entorno son opcionales.

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `MONGODB_URI` | `mongodb://127.0.0.1:27017/beastore` | Cadena de conexión a MongoDB |
| `UPLOAD_DIR` | `public/images` | Carpeta donde se guardan las fotos |

Ejemplo con una instancia autenticada y una carpeta fija:

```bash
export MONGODB_URI="mongodb://usuario:clave@127.0.0.1:27017/beastore?authSource=admin"
export UPLOAD_DIR="/var/lib/beastore/imagenes"
./mvnw spring-boot:run
```

Otros ajustes en `application.properties`: tamaño máximo por archivo (5 MB) y por petición
(10 MB), y caché de Thymeleaf desactivada para desarrollo.

---

## Rutas

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/` | Portada |
| `GET` | `/productos` | Listado completo, del más reciente al más antiguo |
| `GET` | `/productos/crear` | Formulario de alta |
| `POST` | `/productos/crear` | Valida, guarda la foto y crea el documento |
| `GET` | `/productos/edit/{id}` | Formulario de edición con los datos actuales |
| `POST` | `/productos/edit/{id}` | Valida y actualiza; reemplaza la foto solo si se envió una |
| `POST` | `/productos/delete/{id}` | Elimina el documento y su imagen |
| `GET` | `/images/{archivo}` | Sirve una foto subida |

---

## Modelo de datos

Colección `productos`:

```json
{
  "_id": ObjectId("6a7674f89005981c9d98d6ea"),
  "nombre": "Galaxy S21 FE",
  "marca": "Samsung",
  "categoria": "Telefonos",
  "precio": 489999.0,
  "descripcion": "Telefono de 6,4 pulgadas con pantalla de 120 Hz…",
  "fechaCreado": ISODate("2026-08-07T21:14:48.342Z"),
  "imagenArchivo": "1786148088342_galaxy_s21_fe.jpg"
}
```

`imagenArchivo` guarda solo el nombre del archivo. La ruta pública se arma en la vista como
`/images/{imagenArchivo}`, así el directorio de subidas puede moverse sin tocar los documentos.

Categorías disponibles: Notebooks, Telefonos, PCs, Accesorios, Camaras, Impresoras, Otros.
Están definidas en el controlador y se inyectan en los formularios, de modo que agregar una
implica cambiar una sola línea.

---

## Validaciones

| Campo | Regla | Mensaje |
|---|---|---|
| `nombre` | obligatorio | El nombre es requerido |
| `marca` | obligatorio | La marca es requerida |
| `categoria` | obligatoria | La categoria es requerida |
| `precio` | mayor o igual a 0 | El precio no puede ser negativo |
| `descripcion` | entre 10 y 2000 caracteres | La descripcion debe contener por lo menos 10 caracteres |
| `imagenArchivo` | obligatorio al crear, opcional al editar | El archivo de imagen es necesario. |

Se validan en el servidor con Bean Validation, de modo que la regla se cumple aunque el
navegador no coopere.

---

## Capturas

### Catálogo

Cada producto es una fila con su foto, su taxonomía y el precio compuesto como etiqueta de
góndola, en cifras monoespaciadas y alineadas.

![Catálogo](docs/capturas/02-catalogo.png)

### Alta con la etiqueta armándose en vivo

El panel derecho muestra cómo va a quedar el producto mientras se lo carga, incluida la foto
elegida.

![Alta de producto](docs/capturas/04-alta-etiqueta-en-vivo.png)

### Validación

Al fallar, el formulario conserva lo cargado y marca cada campo con su motivo.

![Validación](docs/capturas/05-validacion.png)

### Edición

Los datos llegan cargados, con el identificador y la fecha de alta en solo lectura. La foto se
conserva salvo que se elija una nueva.

![Edición](docs/capturas/06-edicion.png)

### Baja

Pide confirmación nombrando el producto y vuelve al listado con el resultado.

![Baja](docs/capturas/07-baja.png)

### En teléfono

![Vista móvil](docs/capturas/08-movil.png)

---

## Correcciones aplicadas

Estado del proyecto al retomarlo y qué se hizo:

**Las fotos nunca se veían.** Las imágenes se guardaban en `public/images/`, en el sistema de
archivos, pero Spring Boot solo sirve estáticos desde el classpath: cada `<img src="/images/…">`
respondía 404. Se agregó `ConfiguracionWeb`, que publica ese directorio en `/images/**`.

**Las validaciones no se ejecutaban.** El DTO tenía sus anotaciones, pero a los métodos del
controlador les faltaba `@Valid`, así que Spring nunca las evaluaba y se guardaban productos sin
nombre o con precio negativo.

**Excepción de puntero nulo al enviar el formulario sin archivo.** Se llamaba a
`getImagenArchivo().isEmpty()` sin comprobar antes que hubiera archivo.

**Credenciales de MongoDB fijas en el código.** La cadena de conexión traía usuario y contraseña
escritos en `application.properties`, apuntando además a la base `admin`, lo que anulaba el
`spring.data.mongodb.database` declarado más abajo. Ahora es una variable de entorno con un valor
por defecto para desarrollo.

**Nombre de archivo sin sanear.** El nombre original subido se concatenaba a la ruta tal cual. Se
normaliza y se verifica que el destino quede dentro del directorio de subidas.

**El formulario de edición se rompía al fallar la validación.** La vista esperaba el atributo
`productoDTO` y el controlador publicaba `productosDTO`; además el producto no volvía al modelo,
así que el reintento fallaba al pintar la foto. Se unificaron los nombres y se repone el modelo.

**Errores que devolvían un 500.** Un identificador inexistente o mal formado terminaba en una
`RuntimeException`. Ahora hay manejadores que redirigen al listado con un aviso, incluido el caso
de una imagen que supera el tamaño permitido.

**`ProductoService` estaba vacío.** El último commit del repositorio ("Translado a servicios")
había creado el archivo sin contenido, con toda la lógica todavía en el controlador. Se completó
la capa: el controlador quedó en rutas y validación, y el servicio concentra persistencia y
archivos.

**Fecha sin formato y sin protección ante nulos.** El listado hacía
`fechaCreado.toString().substring(0,10)`. Se reemplazó por formato localizado con verificación
previa.

**Codificación rota en `application.properties`.** Los comentarios tenían caracteres `?` en lugar
de acentos.

**Interfaz.** Se reemplazó Bootstrap por una hoja de estilos propia, se rehicieron las cuatro
pantallas sobre fragmentos de Thymeleaf compartidos, se agregaron avisos de resultado, estado
vacío y la vista previa de la etiqueta.

---

## Próximos pasos

- Buscador y filtro por categoría, con paginación en el listado
- Verificación del tipo real de la imagen, más allá de la extensión
- Pruebas de integración con Testcontainers sobre una instancia real de MongoDB
- Autenticación con Spring Security para separar lectura de administración
- Almacenamiento de imágenes en un servicio externo, para poder escalar a más de una instancia

---

## Licencia

Proyecto personal de práctica, sin licencia definida.
