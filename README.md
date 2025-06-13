# Microservicio Multimedia

Un microservicio en Java/JakartaEE para la gestión de perfiles de músicos, valoraciones y contenido multimedia con integración MongoDB.

## Descripción General

Este microservicio proporciona APIs para:
- Gestionar perfiles de músicos con información biográfica, géneros musicales e instrumentos
- Manejar valoraciones y reseñas para músicos
- Subir, almacenar y recuperar archivos multimedia (audio, video, imágenes)
- Almacenar URLs de imágenes para perfiles de músicos

## Tecnologías Utilizadas

- Java 17
- Jakarta EE 11
- MongoDB (con GridFS para almacenamiento de archivos)
- Maven

## Requisitos

- JDK 17 o superior
- Servidor MongoDB
- Maven 3.6+
- Servidor compatible con Jakarta EE (Tomcat 11 recomendado)

## Configuración de MongoDB

### Instalación de MongoDB

1. Descarga MongoDB Community Server desde [el sitio oficial](https://www.mongodb.com/try/download/community)
2. Instala siguiendo las instrucciones para tu sistema operativo
3. Verifica la instalación ejecutando `mongosh` en tu terminal

### Creación de Base de Datos y Colecciones

1. Accede a MongoDB usando el shell:
   ```
   mongosh
   ```

2. Crea y selecciona la base de datos:
   ```javascript
   use multimedia_db
   ```

3. Crea las colecciones necesarias (aunque MongoDB las crea automáticamente, es recomendable crearlas explícitamente):
   ```javascript
   db.createCollection("profiles")
   db.createCollection("ratings")
   db.createCollection("fs.files")
   db.createCollection("fs.chunks")
   ```

4. Verifica la creación de las colecciones:
   ```javascript
   show collections
   ```

### Inserción de Datos de Prueba

Para insertar perfiles de músicos con URLs de imágenes:

```javascript
db.profiles.insertMany([
  {
    userId: "user001",
    name: "John Smith",
    biography: "Guitarrista callejero con más de 10 años de experiencia.",
    imageUrl: "https://cdn.pixabay.com/photo/2014/10/11/22/20/guitar-case-485112_1280.jpg",
    genres: ["Jazz", "Blues"],
    instruments: ["Guitarra", "Armónica"],
    createdAt: new Date(),
    updatedAt: new Date(),
    averageRating: 4.7,
    totalRatings: 45
  },
  {
    userId: "user002",
    name: "Alex Rock",
    biography: "Rockstar especializado en música electrónica y efectos visuales.",
    imageUrl: "https://cdn.pixabay.com/photo/2015/03/08/17/25/musician-664432_640.jpg",
    genres: ["Rock", "Electrónica"],
    instruments: ["Guitarra Eléctrica", "Sintetizador"],
    createdAt: new Date(),
    updatedAt: new Date(),
    averageRating: 4.2,
    totalRatings: 28
  }
])
```

## Configuración del Proyecto

Antes de ejecutar la aplicación, crea un archivo `.env` en la raíz del proyecto basado en la plantilla `.env.template`. Este archivo debe contener:

```
MONGODB_CONNECTION_STRING=mongodb://localhost:27017
MONGODB_DATABASE=multimedia_db
MAX_FILE_SIZE=10485760
ALLOWED_FILE_TYPES=mp3,mp4,jpg,jpeg,png
UPLOAD_TEMP_DIR=/ruta/a/directorio/temporal
```

Ajusta los valores según sea necesario para tu entorno.

## Compilación y Despliegue

Para compilar el proyecto:

```bash
mvn clean package
```

Esto crea un archivo WAR en el directorio `target` que puede ser desplegado en cualquier servidor compatible con Jakarta EE (por ejemplo, Tomcat 11, Payara, Glassfish, TomEE).

Para desplegar en Tomcat:
1. Copia el archivo WAR (`Multimedia-ms-1.0-SNAPSHOT.war`) a la carpeta `webapps` de Tomcat
2. Renombra el archivo a `multimedia.war` para una URL más sencilla
3. Inicia Tomcat si no está ya en ejecución

## Documentación de la API

### URL Base

Todos los endpoints de la API son accesibles bajo:

```
/multimedia/
```

### API de Perfiles de Músicos

#### Obtener Todos los Perfiles

```
GET /profiles
```

Devuelve una lista de todos los perfiles de músicos.

#### Obtener Perfil por ID

```
GET /profiles/{id}
```

Devuelve un único perfil de músico con el ID especificado.

#### Obtener Perfil por ID de Usuario

```
GET /profiles/user/{userId}
```

Devuelve el perfil de músico asociado con el ID de usuario especificado.

#### Crear Perfil

```
POST /profiles
```

Crea un nuevo perfil de músico.

**Cuerpo de la Solicitud**:
```json
{
  "userId": "user123",
  "artisticName": "John Smith",
  "bio": "Guitarrista de jazz con 10 años de experiencia",
  "genre": "Jazz",
  "imageUrl": "https://cdn.pixabay.com/photo/2014/10/11/22/20/guitar-case-485112_1280.jpg"
}
```

#### Actualizar Perfil

```
PUT /profiles/{id}
```

Actualiza un perfil de músico existente.

**Cuerpo de la Solicitud**:
```json
{
  "artisticName": "John A. Smith",
  "bio": "Biografía actualizada",
  "genre": "Jazz",
  "imageUrl": "https://cdn.pixabay.com/photo/2015/03/08/17/25/musician-664432_640.jpg"
}
```

#### Eliminar Perfil

```
DELETE /profiles/{id}
```

Elimina un perfil de músico.

### API de Valoraciones

#### Obtener Todas las Valoraciones para un Músico

```
GET /ratings/musician/{musicianId}
```

Devuelve todas las valoraciones para el músico especificado.

#### Obtener Valoración por ID

```
GET /ratings/{id}
```

Devuelve una única valoración con el ID especificado.

#### Obtener Valoración por Usuario y Músico

```
GET /ratings/musician/{musicianId}/user/{userId}
```

Devuelve la valoración enviada por un usuario específico para un músico específico.

#### Añadir Valoración

```
POST /ratings
```

Añade una nueva valoración para un músico.

**Cuerpo de la Solicitud**:
```json
{
  "musicianId": "musician123",
  "userId": "user456",
  "rating": 4.5,
  "comment": "Gran actuación, muy talentoso!"
}
```

#### Eliminar Valoración

```
DELETE /ratings/{id}
```

Elimina una valoración.

### API Multimedia

#### Obtener Todos los Archivos de un Músico

```
GET /multimedia/musician/{musicianId}?publicOnly=true|false
```

Devuelve todos los archivos multimedia para el músico especificado. Usa el parámetro de consulta `publicOnly` para filtrar solo los archivos públicos.

#### Obtener Metadatos de Archivo

```
GET /multimedia/{id}
```

Devuelve metadatos para un archivo específico.

#### Descargar Archivo

```
GET /multimedia/{id}/download
```

Descarga el archivo con el ID especificado.

#### Subir Archivo

```
POST /multimedia/upload
```

Sube un nuevo archivo. Debe enviarse como `multipart/form-data`.

**Campos del Formulario**:
- `file`: El archivo para subir
- `musicianId`: ID del músico que posee este archivo
- `title`: Título del archivo
- `description`: Descripción opcional
- `isPublic`: Si el archivo debe ser accesible públicamente (`true` o `false`)

#### Actualizar Metadatos de Archivo

```
PUT /multimedia/{id}
```

Actualiza metadatos para un archivo existente.

**Cuerpo de la Solicitud**:
```json
{
  "title": "Título actualizado",
  "description": "Descripción actualizada",
  "isPublic": true
}
```

#### Eliminar Archivo

```
DELETE /multimedia/{id}
```

Elimina un archivo.

## Manejo de Errores

Todos los endpoints de la API devuelven respuestas de error estandarizadas en el siguiente formato:

```json
{
  "error": "Descripción del mensaje de error"
}
```

Códigos de estado HTTP comunes:
- 200: Éxito
- 201: Creado exitosamente
- 400: Solicitud incorrecta (entrada inválida)
- 404: Recurso no encontrado
- 409: Conflicto
- 500: Error del servidor

## Consideraciones de Seguridad

Este microservicio no maneja la autenticación directamente. Está diseñado para funcionar dentro de una arquitectura de microservicios más grande donde la autenticación es manejada por un servicio separado.

Para despliegue en producción, considera:
- Implementar autenticación de gateway de API
- Configurar CORS
- Usar HTTPS
- Implementar limitación de velocidad

## Integración con Users-microservice

Este microservicio está diseñado para complementar el proyecto Users-microservice. Los IDs de usuario referenciados en este servicio deberían corresponder a usuarios válidos en el Users-microservice.

## Características de URLs de Imágenes

Desde la versión más reciente, el sistema admite el almacenamiento de URLs de imágenes externas para perfiles de músicos. Esto ofrece varias ventajas:

- Menor uso de almacenamiento en la base de datos
- Carga más rápida de imágenes
- Posibilidad de usar CDNs para mejor rendimiento

Para actualizar perfiles existentes con URLs de imágenes, puede usar:

```javascript
// En la shell de MongoDB
db.profiles.updateMany(
  { imageUrl: null },
  { $set: { 
      imageUrl: "https://cdn.pixabay.com/photo/2014/10/11/22/20/guitar-case-485112_1280.jpg"
    }
  }
)
```

## Solución de Problemas

### Error 500 en Endpoints

Si los endpoints devuelven errores 500 después de la configuración inicial:
1. Asegúrese de que las colecciones `profiles`, `ratings`, `fs.files` y `fs.chunks` existen en la base de datos
2. Verifique que el archivo `.env` contiene las variables correctas y está en la ubicación adecuada
3. Compruebe los logs del servidor para mensajes de error específicos

### Problemas de Conexión a MongoDB

Si hay problemas conectando con la base de datos:
1. Verifique que MongoDB está en ejecución (`mongosh`)
2. Confirme que la cadena de conexión en `.env` es correcta
3. Asegúrese de que no hay restricciones de firewall bloqueando la conexión

## Desarrollo Futuro

Áreas planificadas para futuras mejoras:
- Sistema de gestión de eventos musicales
- Integración con servicios de streaming
- Búsqueda avanzada de perfiles por género o instrumentos
- Estadísticas y análisis de interacciones
