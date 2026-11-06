# Guía de archivos de SIGELAB Móvil

La aplicación Android consume una API REST Java/Spring Boot. La API aplica las reglas de negocio y accede a SQL Server, al servicio facial y al correo SMTP.

Cada archivo nuevo o modificado se publica con un commit individual que resume su función. Las carpetas de compilación, registros, configuración privada y referencia original se excluyen del estado publicado.

| Archivo | Función |
| --- | --- |
| [`.env.example`](../.env.example) | Documenta variables del servidor con valores de ejemplo sin credenciales. |
| [`.gitignore`](../.gitignore) | Excluye credenciales, cachés y resultados de compilación del repositorio. |
| [`README.md`](../README.md) | Explica instalación, compilación, configuración y funciones de SIGELAB Móvil. |
| [`api/build.gradle.kts`](../api/build.gradle.kts) | Configura compilación Java, dependencias Spring Boot y pruebas de la API con Gradle. |
| [`api/database/04_sigelab_generar_usuario.sql`](../api/database/04_sigelab_generar_usuario.sql) | Define la generación automática de nombres de usuario del esquema de escritorio. |
| [`api/database/05_sigelab_hora_guatemala.sql`](../api/database/05_sigelab_hora_guatemala.sql) | Ajusta valores de fecha y vistas SQL a la hora de Guatemala. |
| [`api/database/verificar_esquema.sql`](../api/database/verificar_esquema.sql) | Consulta el esquema compartido para comprobar tablas, columnas y vistas necesarias. |
| [`api/pom.xml`](../api/pom.xml) | Permite compilar y probar la API con Maven y habilita el servicio de correo. |
| [`api/settings.gradle.kts`](../api/settings.gradle.kts) | Define el proyecto Gradle independiente de la API. |
| [`api/src/main/java/SIGELAB/enums/AccionAuditoria.java`](../api/src/main/java/SIGELAB/enums/AccionAuditoria.java) | Enumera las acciones que pueden registrarse en auditoría. |
| [`api/src/main/java/SIGELAB/enums/CondicionDevolucion.java`](../api/src/main/java/SIGELAB/enums/CondicionDevolucion.java) | Define los valores permitidos para condicion devolucion. |
| [`api/src/main/java/SIGELAB/enums/EstadoDetalle.java`](../api/src/main/java/SIGELAB/enums/EstadoDetalle.java) | Define los valores permitidos para estado detalle. |
| [`api/src/main/java/SIGELAB/enums/EstadoEquipo.java`](../api/src/main/java/SIGELAB/enums/EstadoEquipo.java) | Define los valores permitidos para estado equipo. |
| [`api/src/main/java/SIGELAB/enums/EstadoMantenimiento.java`](../api/src/main/java/SIGELAB/enums/EstadoMantenimiento.java) | Define los valores permitidos para estado mantenimiento. |
| [`api/src/main/java/SIGELAB/enums/EstadoPrestamo.java`](../api/src/main/java/SIGELAB/enums/EstadoPrestamo.java) | Define los valores permitidos para estado prestamo. |
| [`api/src/main/java/SIGELAB/enums/EstadoRegistro.java`](../api/src/main/java/SIGELAB/enums/EstadoRegistro.java) | Define los valores permitidos para estado registro. |
| [`api/src/main/java/SIGELAB/enums/ResultadoMantenimiento.java`](../api/src/main/java/SIGELAB/enums/ResultadoMantenimiento.java) | Define los valores permitidos para resultado mantenimiento. |
| [`api/src/main/java/SIGELAB/enums/RolUsuario.java`](../api/src/main/java/SIGELAB/enums/RolUsuario.java) | Define los valores permitidos para rol usuario. |
| [`api/src/main/java/SIGELAB/enums/TipoMantenimiento.java`](../api/src/main/java/SIGELAB/enums/TipoMantenimiento.java) | Define los valores permitidos para tipo mantenimiento. |
| [`api/src/main/java/SIGELAB/enums/TipoSolicitante.java`](../api/src/main/java/SIGELAB/enums/TipoSolicitante.java) | Define los valores permitidos para tipo solicitante. |
| [`api/src/main/java/SIGELAB/facial/VerificacionFacial.java`](../api/src/main/java/SIGELAB/facial/VerificacionFacial.java) | Define los resultados permitidos de la verificación facial del servidor. |
| [`api/src/main/java/SIGELAB/model/Auditoria.java`](../api/src/main/java/SIGELAB/model/Auditoria.java) | Representa los datos de las acciones de auditoría. |
| [`api/src/main/java/SIGELAB/model/DetallePrestamo.java`](../api/src/main/java/SIGELAB/model/DetallePrestamo.java) | Representa los datos de los equipos incluidos en un préstamo. |
| [`api/src/main/java/SIGELAB/model/Equipo.java`](../api/src/main/java/SIGELAB/model/Equipo.java) | Representa los datos de los equipos del inventario. |
| [`api/src/main/java/SIGELAB/model/Laboratorio.java`](../api/src/main/java/SIGELAB/model/Laboratorio.java) | Representa los datos de los laboratorios. |
| [`api/src/main/java/SIGELAB/model/Mantenimiento.java`](../api/src/main/java/SIGELAB/model/Mantenimiento.java) | Representa los datos de los registros de mantenimiento. |
| [`api/src/main/java/SIGELAB/model/Prestamo.java`](../api/src/main/java/SIGELAB/model/Prestamo.java) | Representa los datos de los préstamos. |
| [`api/src/main/java/SIGELAB/model/ResultadoReporte.java`](../api/src/main/java/SIGELAB/model/ResultadoReporte.java) | Representa las columnas y filas de un reporte genérico. |
| [`api/src/main/java/SIGELAB/model/ResumenDashboard.java`](../api/src/main/java/SIGELAB/model/ResumenDashboard.java) | Transporta los indicadores numéricos del panel. |
| [`api/src/main/java/SIGELAB/model/Solicitante.java`](../api/src/main/java/SIGELAB/model/Solicitante.java) | Representa los datos de los solicitantes. |
| [`api/src/main/java/SIGELAB/model/TipoEquipo.java`](../api/src/main/java/SIGELAB/model/TipoEquipo.java) | Representa los datos de los tipos de equipo. |
| [`api/src/main/java/SIGELAB/model/Usuario.java`](../api/src/main/java/SIGELAB/model/Usuario.java) | Representa los datos de las cuentas de usuario. |
| [`api/src/main/java/SIGELAB/repository/AuditoriaRepository.java`](../api/src/main/java/SIGELAB/repository/AuditoriaRepository.java) | Accede mediante consultas SQL parametrizadas a las acciones de auditoría. |
| [`api/src/main/java/SIGELAB/repository/DevolucionRepository.java`](../api/src/main/java/SIGELAB/repository/DevolucionRepository.java) | Accede mediante consultas SQL parametrizadas a las devoluciones. |
| [`api/src/main/java/SIGELAB/repository/EquipoRepository.java`](../api/src/main/java/SIGELAB/repository/EquipoRepository.java) | Accede mediante consultas SQL parametrizadas a los equipos del inventario. |
| [`api/src/main/java/SIGELAB/repository/LaboratorioRepository.java`](../api/src/main/java/SIGELAB/repository/LaboratorioRepository.java) | Accede mediante consultas SQL parametrizadas a los laboratorios. |
| [`api/src/main/java/SIGELAB/repository/MantenimientoRepository.java`](../api/src/main/java/SIGELAB/repository/MantenimientoRepository.java) | Accede mediante consultas SQL parametrizadas a los registros de mantenimiento. |
| [`api/src/main/java/SIGELAB/repository/PrestamoRepository.java`](../api/src/main/java/SIGELAB/repository/PrestamoRepository.java) | Accede mediante consultas SQL parametrizadas a los préstamos. |
| [`api/src/main/java/SIGELAB/repository/ReporteFiltroRepository.java`](../api/src/main/java/SIGELAB/repository/ReporteFiltroRepository.java) | Construye consultas SQL parametrizadas de reportes con filtros. |
| [`api/src/main/java/SIGELAB/repository/ReporteRepository.java`](../api/src/main/java/SIGELAB/repository/ReporteRepository.java) | Consulta los reportes e historial del esquema compartido. |
| [`api/src/main/java/SIGELAB/repository/SolicitanteRepository.java`](../api/src/main/java/SIGELAB/repository/SolicitanteRepository.java) | Accede mediante consultas SQL parametrizadas a los solicitantes. |
| [`api/src/main/java/SIGELAB/repository/TipoEquipoRepository.java`](../api/src/main/java/SIGELAB/repository/TipoEquipoRepository.java) | Accede mediante consultas SQL parametrizadas a los tipos de equipo. |
| [`api/src/main/java/SIGELAB/repository/UsuarioRepository.java`](../api/src/main/java/SIGELAB/repository/UsuarioRepository.java) | Accede mediante consultas SQL parametrizadas a las cuentas de usuario. |
| [`api/src/main/java/SIGELAB/service/AuditoriaService.java`](../api/src/main/java/SIGELAB/service/AuditoriaService.java) | Registra acciones y consulta la auditoría con permisos y filtros. |
| [`api/src/main/java/SIGELAB/service/AuthService.java`](../api/src/main/java/SIGELAB/service/AuthService.java) | Valida credenciales y estado de cuenta; exige aprobación facial antes de abrir sesión. |
| [`api/src/main/java/SIGELAB/service/CorreoService.java`](../api/src/main/java/SIGELAB/service/CorreoService.java) | Envía avisos SMTP de acceso, registro, rechazo facial, préstamos y devoluciones. |
| [`api/src/main/java/SIGELAB/service/DashboardService.java`](../api/src/main/java/SIGELAB/service/DashboardService.java) | Obtiene los indicadores resumidos del panel. |
| [`api/src/main/java/SIGELAB/service/DevolucionService.java`](../api/src/main/java/SIGELAB/service/DevolucionService.java) | Registra devoluciones parciales o finales, actualiza equipos y notifica después de guardar. |
| [`api/src/main/java/SIGELAB/service/EquipoService.java`](../api/src/main/java/SIGELAB/service/EquipoService.java) | Valida inventario, modificaciones y desactivación de equipos. |
| [`api/src/main/java/SIGELAB/service/LaboratorioService.java`](../api/src/main/java/SIGELAB/service/LaboratorioService.java) | Valida la administración y desactivación de laboratorios. |
| [`api/src/main/java/SIGELAB/service/MantenimientoService.java`](../api/src/main/java/SIGELAB/service/MantenimientoService.java) | Aplica las reglas de apertura y cierre de mantenimiento y sus cambios de estado. |
| [`api/src/main/java/SIGELAB/service/PrestamoService.java`](../api/src/main/java/SIGELAB/service/PrestamoService.java) | Valida disponibilidad y confirma préstamos completos en una transacción antes de notificar. |
| [`api/src/main/java/SIGELAB/service/ReporteService.java`](../api/src/main/java/SIGELAB/service/ReporteService.java) | Valida filtros y permisos para consultar los reportes del sistema. |
| [`api/src/main/java/SIGELAB/service/SolicitanteService.java`](../api/src/main/java/SIGELAB/service/SolicitanteService.java) | Valida datos y administración de los solicitantes de préstamos. |
| [`api/src/main/java/SIGELAB/service/TipoEquipoService.java`](../api/src/main/java/SIGELAB/service/TipoEquipoService.java) | Valida la administración de las categorías de equipos. |
| [`api/src/main/java/SIGELAB/service/UsuarioService.java`](../api/src/main/java/SIGELAB/service/UsuarioService.java) | Valida cuentas, roles y contraseñas; crea usuarios y programa su aviso de registro. |
| [`api/src/main/java/SIGELAB/util/BusinessException.java`](../api/src/main/java/SIGELAB/util/BusinessException.java) | Representa errores de validación o reglas de negocio. |
| [`api/src/main/java/SIGELAB/util/ConexionBD.java`](../api/src/main/java/SIGELAB/util/ConexionBD.java) | Proporciona conexiones del DataSource de Spring a los repositorios SQL. |
| [`api/src/main/java/SIGELAB/util/Configuracion.java`](../api/src/main/java/SIGELAB/util/Configuracion.java) | Resuelve configuración privada con prioridad para las variables de entorno. |
| [`api/src/main/java/SIGELAB/util/DataAccessException.java`](../api/src/main/java/SIGELAB/util/DataAccessException.java) | Encapsula errores de acceso SQL para tratarlos en la API. |
| [`api/src/main/java/SIGELAB/util/Fechas.java`](../api/src/main/java/SIGELAB/util/Fechas.java) | Convierte y presenta fechas utilizadas por las reglas del sistema. |
| [`api/src/main/java/SIGELAB/util/Seguridad.java`](../api/src/main/java/SIGELAB/util/Seguridad.java) | Calcula hashes de contraseña compatibles con el sistema de escritorio. |
| [`api/src/main/java/SIGELAB/util/Sesion.java`](../api/src/main/java/SIGELAB/util/Sesion.java) | Aísla el usuario activo por solicitud y comprueba permisos según rol. |
| [`api/src/main/java/SIGELAB/util/Transaccion.java`](../api/src/main/java/SIGELAB/util/Transaccion.java) | Ejecuta operaciones SQL con confirmación o reversión ante errores. |
| [`api/src/main/java/gt/edu/sigelab/api/ApiController.java`](../api/src/main/java/gt/edu/sigelab/api/ApiController.java) | Gestiona credenciales, tokens pendientes, sesiones, límites de intentos y avisos de acceso exitoso. |
| [`api/src/main/java/gt/edu/sigelab/api/ApiErrors.java`](../api/src/main/java/gt/edu/sigelab/api/ApiErrors.java) | Convierte errores de negocio y acceso a respuestas HTTP comprensibles. |
| [`api/src/main/java/gt/edu/sigelab/api/FacialController.java`](../api/src/main/java/gt/edu/sigelab/api/FacialController.java) | Protege y conecta los contratos faciales con el servicio existente sin exponer su clave a Android. |
| [`api/src/main/java/gt/edu/sigelab/api/ModulesController.java`](../api/src/main/java/gt/edu/sigelab/api/ModulesController.java) | Expone los módulos de inventario, usuarios, préstamos, devoluciones, mantenimiento y reportes. |
| [`api/src/main/java/gt/edu/sigelab/api/ServerConfiguration.java`](../api/src/main/java/gt/edu/sigelab/api/ServerConfiguration.java) | Conecta las reglas heredadas con las propiedades privadas y variables del servidor. |
| [`api/src/main/java/gt/edu/sigelab/api/SessionFilter.java`](../api/src/main/java/gt/edu/sigelab/api/SessionFilter.java) | Valida la sesión para cada solicitud protegida y limpia su identidad al finalizar. |
| [`api/src/main/java/gt/edu/sigelab/api/SigelabApiApplication.java`](../api/src/main/java/gt/edu/sigelab/api/SigelabApiApplication.java) | Inicia el servidor Spring Boot. |
| [`api/src/main/resources/application.properties`](../api/src/main/resources/application.properties) | Configura puerto, conexión SQL, límites de imágenes y carga opcional de configuración privada. |
| [`api/src/test/java/SIGELAB/service/CorreoServiceTest.java`](../api/src/test/java/SIGELAB/service/CorreoServiceTest.java) | Comprueba destinatarios, contenido HTML, fechas y omisión de avisos cuando no corresponde. |
| [`api/src/test/java/SIGELAB/service/RegistroCorreoTest.java`](../api/src/test/java/SIGELAB/service/RegistroCorreoTest.java) | Comprueba que solo se notifiquen las cuentas guardadas y con su usuario generado. |
| [`api/src/test/java/gt/edu/sigelab/api/AccessTest.java`](../api/src/test/java/gt/edu/sigelab/api/AccessTest.java) | Comprueba permisos, aislamiento de identidad, privacidad y avisos tras autenticación. |
| [`api/src/test/java/gt/edu/sigelab/api/ApplicationTest.java`](../api/src/test/java/gt/edu/sigelab/api/ApplicationTest.java) | Comprueba el arranque del contexto de la API. |
| [`api/src/test/java/gt/edu/sigelab/api/FacialTest.java`](../api/src/test/java/gt/edu/sigelab/api/FacialTest.java) | Comprueba autorización facial, contratos del proxy y avisos de rechazo sin falsos positivos por caídas. |
| [`api/src/test/java/gt/edu/sigelab/api/WorkflowTest.java`](../api/src/test/java/gt/edu/sigelab/api/WorkflowTest.java) | Comprueba transacciones, estados y notificaciones de préstamos, devoluciones y mantenimiento. |
| [`app/build.gradle.kts`](../app/build.gradle.kts) | Configura Android, versión de la app, URL de la API y dependencias de cámara y QR. |
| [`app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | Declara permisos de red y cámara y registra las actividades de Android. |
| [`app/src/main/java/gt/edu/sigelab/mobile/Api.kt`](../app/src/main/java/gt/edu/sigelab/mobile/Api.kt) | Realiza solicitudes HTTP asíncronas a la API y gestiona respuestas y sesiones vencidas. |
| [`app/src/main/java/gt/edu/sigelab/mobile/MainActivity.kt`](../app/src/main/java/gt/edu/sigelab/mobile/MainActivity.kt) | Coordina la sesión, el tema, la captura facial y la navegación principal. |
| [`app/src/main/java/gt/edu/sigelab/mobile/navegacion/AppNavegacion.kt`](../app/src/main/java/gt/edu/sigelab/mobile/navegacion/AppNavegacion.kt) | Conecta las rutas de los módulos y pantallas según la sesión. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/login/LoginScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/login/LoginScreen.kt) | Muestra el login adaptable con iconos, visibilidad de contraseña y cambio de tema. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/menu/MainMenuScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/menu/MainMenuScreen.kt) | Presenta el menú de módulos según el rol del usuario. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/CatalogScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/CatalogScreen.kt) | Implementa formularios y listas de catálogos, usuarios, equipos, QR y administración facial. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/CommonUi.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/CommonUi.kt) | Reúne componentes compartidos de formularios, filtros, fechas y mensajes. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/LoansScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/LoansScreen.kt) | Presenta préstamos, selección de equipos y devoluciones parciales o completas. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/MaintenanceScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/MaintenanceScreen.kt) | Presenta el registro y cierre de mantenimientos. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/ReportPrintAdapter.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/ReportPrintAdapter.kt) | Conecta los reportes PDF con el sistema de impresión Android. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/modules/ReportsScreen.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/modules/ReportsScreen.kt) | Presenta filtros de reportes, exportación CSV/PDF y auditoría. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/theme/SigelabNativeTheme.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/theme/SigelabNativeTheme.kt) | Aplica la misma paleta a vistas nativas de Android y a la captura facial. |
| [`app/src/main/java/gt/edu/sigelab/mobile/ui/theme/SigelabTheme.kt`](../app/src/main/java/gt/edu/sigelab/mobile/ui/theme/SigelabTheme.kt) | Aplica en Compose la paleta clara y oscura del escritorio. |
| [`app/src/main/java/sigelab/facial/CapturaFacialActivity.java`](../app/src/main/java/sigelab/facial/CapturaFacialActivity.java) | Controla la cámara frontal, la prueba de vida y los flujos de verificación y registro facial. |
| [`app/src/main/java/sigelab/facial/FacialApiClient.java`](../app/src/main/java/sigelab/facial/FacialApiClient.java) | Envía desafíos, imágenes y operaciones faciales al proxy autenticado de la API. |
| [`app/src/main/res/drawable/ic_arrow_right.xml`](../app/src/main/res/drawable/ic_arrow_right.xml) | Recurso vectorial del icono de continuar. |
| [`app/src/main/res/drawable/ic_eye.xml`](../app/src/main/res/drawable/ic_eye.xml) | Recurso vectorial del icono de mostrar contraseña. |
| [`app/src/main/res/drawable/ic_eye_off.xml`](../app/src/main/res/drawable/ic_eye_off.xml) | Recurso vectorial del icono de ocultar contraseña. |
| [`app/src/main/res/drawable/ic_face_scan.xml`](../app/src/main/res/drawable/ic_face_scan.xml) | Recurso vectorial del icono de verificación facial. |
| [`app/src/main/res/drawable/ic_lab.xml`](../app/src/main/res/drawable/ic_lab.xml) | Recurso vectorial del icono de laboratorios. |
| [`app/src/main/res/drawable/ic_lock.xml`](../app/src/main/res/drawable/ic_lock.xml) | Recurso vectorial del icono de contraseña. |
| [`app/src/main/res/drawable/ic_moon.xml`](../app/src/main/res/drawable/ic_moon.xml) | Recurso vectorial del icono de tema oscuro. |
| [`app/src/main/res/drawable/ic_sun.xml`](../app/src/main/res/drawable/ic_sun.xml) | Recurso vectorial del icono de tema claro. |
| [`app/src/main/res/drawable/ic_user.xml`](../app/src/main/res/drawable/ic_user.xml) | Recurso vectorial del icono de usuario. |
| [`app/src/main/res/layout/activity_captura_facial.xml`](../app/src/main/res/layout/activity_captura_facial.xml) | Define la vista de cámara, controles e indicaciones de captura facial. |
| [`build.gradle.kts`](../build.gradle.kts) | Define las versiones de los plugins Android y Compose del proyecto raíz. |
| [`docs/ARCHIVOS.md`](../docs/ARCHIVOS.md) | Explica archivo por archivo la estructura y la función del código publicado. |
| [`docs/CORREOS.md`](../docs/CORREOS.md) | Documenta eventos de correo, configuración privada y verificaciones SMTP realizadas. |
| [`docs/MIGRACION_ESCRITORIO.md`](../docs/MIGRACION_ESCRITORIO.md) | Relaciona las funciones del escritorio con su implementación y validación móvil. |
| [`gradle.properties`](../gradle.properties) | Configura memoria de Gradle y soporte AndroidX. |
| [`gradle/gradle-daemon-jvm.properties`](../gradle/gradle-daemon-jvm.properties) | Define la versión de Java utilizada por el daemon de Gradle. |
| [`gradle/wrapper/gradle-wrapper.jar`](../gradle/wrapper/gradle-wrapper.jar) | Permite descargar y ejecutar la versión de Gradle definida en el proyecto. |
| [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties) | Define la distribución de Gradle utilizada por el wrapper. |
| [`gradlew`](../gradlew) | Inicia Gradle desde sistemas Unix usando el wrapper. |
| [`gradlew.bat`](../gradlew.bat) | Inicia Gradle desde Windows usando el wrapper. |
| [`settings.gradle.kts`](../settings.gradle.kts) | Configura repositorios de dependencias y módulos del proyecto Android. |

## Archivos locales excluidos

`api/application-local.properties` contiene la configuración privada del equipo. Las variables del servidor se administran fuera de Git. No se publican contraseñas, claves faciales, JAR generados, APK, cachés ni la copia original del escritorio. Los scripts SQL incluidos documentan el esquema compartido; no se ejecutan automáticamente.
