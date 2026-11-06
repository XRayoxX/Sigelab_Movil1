# Notificaciones de SIGELAB

El correo se envía desde la API, con las mismas notificaciones previstas por CorreoService del escritorio. El teléfono no guarda la clave SMTP.

| Evento | Destinatario | Contenido |
| --- | --- | --- |
| Inicio de sesión exitoso | Dueño de la cuenta | Usuario y fecha/hora, después de aprobar el rostro |
| Cuenta registrada | Nuevo usuario | Nombre de usuario generado e instrucciones de acceso |
| Préstamo confirmado | Solicitante | Número de préstamo, códigos y nombres de equipos, fecha y devolución esperada |
| Equipo devuelto | Solicitante | Equipo, condición bueno/dañado y cantidad pendiente; confirma finalización al devolver el último |
| Rostro/prueba de vida rechazados tras la contraseña | Dueño de la cuenta | Usuario, fecha/hora y aviso del intento no reconocido |

Las fechas se muestran en hora de Guatemala. Préstamos y devoluciones notifican después del commit. Si el solicitante no tiene correo, se omite el envío. Un fallo de SMTP se registra y no revierte la operación. No se crean avisos de préstamo o devolución si se produjo rollback. Cancelar la captura o una caída de red del servicio facial no genera un aviso de rostro no reconocido.

Se usa Gmail SMTP en puerto 587, autenticación y STARTTLS obligatorio, comprobación de certificado y límites de espera de 15 segundos. La contraseña debe ser una contraseña de aplicación del correo. Se conserva el formato HTML del escritorio y se escapan los datos de nombres/equipos.

## Configuración privada

Variables en Azure:

- SIGELAB_CORREO_HABILITADO=true
- SIGELAB_CORREO_REMITENTE
- SIGELAB_CORREO_CONTRASENA

Alternativamente, para ejecutar la API local, se admiten sigelab.correo.habilitado, sigelab.correo.remitente y sigelab.correo.contrasena en api/application-local.properties (ignorado por Git).

## Comprobación

Las pruebas verifican contenido, destinatarios y que los avisos se generen después de guardar las operaciones o completar la autenticación facial. Una contraseña válida por sí sola no genera el aviso de acceso exitoso. El registro corresponde a la creación de cuentas por ADMIN; no existe registro público. Ningún aviso incluye contraseñas, hashes ni tokens.

La aceptación SMTP se registra con el identificador del mensaje. Esto confirma la recepción por Gmail, no garantiza su ubicación en la bandeja de entrada. Revisar también spam.

## Resultado de la revisión del 06/11/2026

La primera prueba recibió `535 5.7.8 Username and Password not accepted` de Gmail. Tras actualizar la contraseña de aplicación privada, el envío real desde la misma implementación de CorreoService fue aceptado por Gmail. El destinatario de prueba fue el propio buzón de SIGELAB; no se crearon usuarios, préstamos ni devoluciones ficticios. La aceptación SMTP no confirma la ubicación del mensaje en la bandeja.

Las 36 pruebas de la API pasaron. La configuración SMTP se actualizó en Azure y el entorno de administración del servidor también entregó un mensaje de prueba aceptado por Gmail. Se publicó la API revisada y Azure confirmó el arranque correcto, con una instancia exitosa y ninguna fallida. La configuración SMTP permanece excluida de Git y del APK.
