# SIGELAB Móvil — primera entrega

Monorepositorio de **Android (Kotlin + Jetpack Compose)** y **API REST (Java 21 + Spring Boot)** para reutilizar la base de datos **Azure SQL** del SIGELAB de escritorio.

## Estado de implementación

- ✅ Pantalla de inicio de sesión con usuario y contraseña.
- ✅ API `POST /api/auth/login` contra `dbo.Usuario` + `dbo.Rol`.
- ✅ Sesión temporal con token opaco de 1 hora y `POST /api/auth/logout`.
- ✅ Panel de estadísticas (`GET /api/dashboard`) mediante `dbo.vw_Dashboard`.
- ✅ Proyecto organizado para control de versiones con Git.
- ⏳ Equipos, QR, laboratorios, tipos, solicitantes, préstamos, devoluciones, mantenimiento, usuarios, reportes detallados y auditoría: **aún no implementados**.
- ⏳ Reconocimiento facial: no está implementado.

**Esta versión es una base de desarrollo, no un producto apto para Internet público.** El modelo de contraseñas SHA-256 sin sal corresponde al sistema antiguo y debe migrarse a Argon2id/bcrypt. Faltan limitación de intentos, auditoría de accesos, almacenamiento distribuido de sesiones, rotación de credenciales y endurecimiento de la API antes de producción.

## 1. Ejecutar la API

Prerequisitos: JDK 21, Maven 3.9+, acceso de red a Azure SQL y credenciales de un usuario SQL con permisos mínimos sobre las tablas/vistas utilizadas.

En PowerShell, en la carpeta `api`:

```powershell
$env:SIGELAB_DB_URL = 'jdbc:sqlserver://TU-SERVIDOR.database.windows.net:1433;databaseName=TU-BASE;encrypt=true;trustServerCertificate=false;loginTimeout=30'
$env:SIGELAB_DB_USER = 'TU-USUARIO'
$env:SIGELAB_DB_PASSWORD = 'TU-CONTRASENA'
mvn spring-boot:run
```

**No agregues credenciales a GitHub** ni uses el usuario administrador (`sa`) para la API. En producción usa un gestor de secretos. Revisa que la base real tenga `dbo.Usuario`, `dbo.Rol` y `dbo.vw_Dashboard`; los cambios recientes de tu esquema Azure pueden requerir ajustar nombres de columnas.

Prueba local:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/health'
```

La ruta de salud solo indica que el proceso inició; no valida por sí sola la conexión a Azure SQL. Para validar dicha conexión, hay que probar el login con un usuario de pruebas.

## 2. Abrir Android Studio

1. Abre la carpeta **raíz** `SIGELAB-Mobile` desde `File > Open` (no abras solo `app`).
2. Usa Android Studio compatible con Android Gradle Plugin 9.4; JDK 17+ y SDK API 37.
3. En la configuración de Gradle, crea una propiedad local `sigelabApiUrl` con la **URL HTTPS pública o privada accesible desde tu teléfono/emulador**. Por ejemplo, en el `gradle.properties` de usuario de Gradle (fuera del repositorio): `sigelabApiUrl=https://api.tudominio.com/`.
4. Sin esa propiedad, la aplicación usa el marcador `https://api.example.invalid/` y el inicio de sesión no podrá funcionar.
5. Sincroniza Gradle y ejecuta en el emulador o un Android físico.

La carpeta incluye la configuración del wrapper, pero **no** los ejecutables `gradlew`/`gradlew.bat` ni `gradle-wrapper.jar`; no fue posible generarlos sin una instalación de Gradle. Si Android Studio no los genera al abrir el proyecto, instala Gradle 9.6.0 y ejecuta `gradle wrapper --gradle-version 9.6.0` antes de importar.

La aplicación requiere una API servida por **HTTPS con certificado confiable**. El endpoint HTTP local es solo para comprobar la API desde el propio servidor; Android bloquea tráfico sin cifrar por defecto.

## 3. Crear y usar GitHub

Desde PowerShell en `SIGELAB-Mobile`:

```powershell
git init
git add .
git commit -m "feat: iniciar app Android y API para Azure SQL"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/SIGELAB-Mobile.git
git push -u origin main
```

Primero crea un repositorio **vacío y privado** con ese nombre en GitHub. En otra computadora:

```powershell
git clone https://github.com/TU-USUARIO/SIGELAB-Mobile.git
```

Abre la carpeta clonada en Android Studio; cada computadora configura su propia URL de API. Usa ramas `feature/...` y Pull Requests para las siguientes funcionalidades.

## 4. Siguientes iteraciones

1. Validar esquema real de Azure SQL y autenticar un usuario de pruebas.
2. Fortalecer seguridad y completar configuración de API HTTPS.
3. Equipos, búsqueda y lectura QR.
4. Préstamos y devoluciones con operaciones transaccionales.
5. Catálogos, solicitantes, mantenimiento, administración, auditoría y reportes.

**No se realizaron conexiones ni modificaciones a Azure SQL durante la preparación de esta entrega.**
