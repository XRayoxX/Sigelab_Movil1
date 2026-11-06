# Migración del escritorio a Android

Referencia: código del archivo SIGELAB.rar recibido el 28/10/2026. Los archivos del archivo comprimido se trataron como material de referencia, sin ejecutar sus scripts ni modificar sus servicios.

## Matriz de funciones

| Escritorio | Android | API | Roles |
| --- | --- | --- | --- |
| Login + verificación facial | Credenciales y CameraX frontal, desafío de giro | auth/login, face/challenge, face/verify | Todos |
| FrmPrincipal | Menú y ocho contadores | dashboard | Todos |
| FrmEquipos | Inventario, formularios, filtros, detalle y QR PNG | equipos, equipos/codigo, equipos/historial | ADMIN, ENCARGADO |
| FrmEscanearQr | Lector QR Android, búsqueda y agregar al préstamo | equipos/codigo | ADMIN, ENCARGADO |
| FrmLaboratorios | Listado, crear, editar, desactivar | laboratorios | ADMIN |
| FrmTiposEquipo | Listado, crear, editar, desactivar | tipos | ADMIN |
| FrmSolicitantes | Listado, crear, editar, desactivar | solicitantes | ADMIN, ENCARGADO |
| FrmUsuarios | CRUD lógico, cambio de contraseña, nombre generado | usuarios | ADMIN |
| Administración facial | Consultar, registrar, reemplazar, eliminar | face/status, face/enroll, face/profile | ADMIN |
| FrmPrestamos | Varios equipos, carné, fecha/hora, filtros y detalles | prestamos | ADMIN, ENCARGADO |
| FrmDevoluciones | Detalles pendientes, condición, observaciones y cierre | devoluciones | ADMIN, ENCARGADO |
| FrmMantenimiento | Ingreso y cierre, historial de cada registro | mantenimiento | ADMIN, ENCARGADO |
| FrmReportes | Nueve consultas, filtros, PDF, CSV e impresión Android | reportes | Todos |
| FrmAuditoria | Consulta con usuario, entidad, acción y fechas | auditoria | ADMIN |
| FrmCalendario | Selector nativo de fecha y hora | Fechas validadas en API | Según módulo |
| TemaSIGELAB | Tema claro/oscuro | Local | Todos |
| CorreoService | Envío desde el servidor tras operación | Variables SMTP del servidor | Operaciones correspondientes |

El botón «Excel» del escritorio exporta CSV; Android conserva ese formato compatible con Excel. El calendario del escritorio es un selector de fechas, por eso se adaptó a los diálogos nativos.

## Operaciones HTTP

Todas las rutas están debajo de /api. Salvo health y auth/login, requieren el token del flujo correspondiente.

- GET /health.
- POST /auth/login: username y password. Devuelve pendingToken y userId; todavía no autoriza módulos.
- GET /face/challenge con Bearer pendingToken.
- POST /face/verify: multipart con challengeToken, mirrored e images; Bearer pendingToken. El servidor toma la identidad del token y solo entrega una sesión tras validar el rostro.
- POST /auth/logout: invalida el token recibido.
- GET /dashboard.
- GET /equipos?q=&estado=&laboratorio=; GET /equipos/codigo?codigo=; GET /equipos/historial?codigo=.
- GET /laboratorios, /tipos, /solicitantes: q opcional; activos=true para los selectores.
- GET /usuarios?q=.
- POST /equipos, /laboratorios, /tipos, /solicitantes: objeto del modelo; ID cero o ausente crea y ID existente actualiza.
- POST /usuarios: usuario, password y confirmation. Contraseña vacía al editar conserva la anterior.
- POST /{laboratorios|tipos|solicitantes|usuarios}/{id}/desactivar.
- GET /prestamos?q=&estado=TODOS|ACTIVO|FINALIZADO|VENCIDO.
- GET /prestamos/{id}/detalles.
- POST /prestamos: solicitanteId, fechaEsperadaDevolucion (ISO local de Guatemala), observaciones y equipos (IDs).
- GET /devoluciones?q=; POST /devoluciones: detalleId, condicion (BUENO|DANADO) y observaciones.
- GET /mantenimiento?q=&estado=; GET /mantenimiento/equipos.
- POST /mantenimiento: equipo con idEquipo, tipo, descripcion y responsable.
- POST /mantenimiento/{id}/finalizar: resultado (REPARADO|NO_REPARABLE) y observaciones.
- GET /reportes?tipo=&laboratorio=&codigo=&estado=&desde=&hasta=.
- GET /auditoria?usuario=&entidad=&accion=&desde=&hasta=.
- GET /face/status/{id}; POST /face/enroll (multipart); DELETE /face/profile/{id}. Requieren sesión administrativa para operar sobre otras personas.

Las listas usan un objeto con items. Los reportes usan columnas y filas. Los filtros de fecha de reportes/auditoría usan dd/MM/yyyy. No se permite editar manualmente el estado del equipo: lo cambian las operaciones de negocio.

## Adaptaciones del servidor

- ConexionBD utiliza el DataSource y el pool de Spring.
- Sesion usa ThreadLocal por solicitud, con limpieza en finally; no comparte al usuario activo entre clientes.
- La API revalida existencia, estado y rol actual en cada solicitud autenticada.
- Los hashes no se serializan en respuestas.
- El login limita intentos por IP/usuario y separa tokens previos al rostro de sesiones autorizadas.
- La clave facial se queda en variables del servidor.
- Las devoluciones bloquean el encabezado del préstamo antes de modificar los detalles para serializar devoluciones simultáneas.
- Los cambios de estado siguen validándose dentro de transacciones y las operaciones fallidas hacen rollback.
- Se conserva la compatibilidad con el esquema y los enums del escritorio.

## Comprobación real pendiente

La validación automática no sustituye estas pruebas de integración en una base de pruebas:

1. Desplegar la nueva API y configurar SQL, HTTPS y el servicio facial.
2. Confirmar que un ADMIN existente con rostro registrado puede iniciar sesión y que un rostro incorrecto se rechaza.
3. Crear un laboratorio, tipo, solicitante y equipo; leer el QR con un teléfono.
4. Prestar dos equipos; devolver uno bueno y otro dañado; confirmar estados y cierre tanto en Android como en escritorio.
5. Hacer mantenimiento preventivo y correctivo; cerrar reparado y no reparable.
6. Ejecutar los nueve reportes, exportar varias páginas y confirmar selección de páginas al imprimir.
7. Consultar auditoría y comprobar los permisos de ENCARGADO y DOCENTE.
8. Comprobar registro/reemplazo/eliminación facial, permisos de cámara, cancelación y correos.

No se ejecutaron cambios de datos ni migraciones de esquema. Tras la autorización expresa del usuario se desplegó la API actualizada en Azure y se verificaron las rutas de salud, incluida la conexión facial con modelo cargado y prueba de vida activa.

## Revisión de integración facial

Se recuperó la configuración real desde el `config.properties` del escritorio durante la migración inicial; queda en `api/application-local.properties` excluido de Git. El XML Android original contiene marcadores de ejemplo, por lo que no se copió su clave de ejemplo al APK.

Se completaron los contratos del cliente original: salud, desafío, registro, verificación, identificación, análisis, estado y eliminación. Identificación y análisis requieren sesión ADMIN/ENCARGADO; no conceden acceso por sí solos. Un desafío de login consulta primero el estado del perfil y debe pertenecer a la identidad del token pendiente. Los errores de calidad y pose del servicio se conservan para mostrarlos en Android.
