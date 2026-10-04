# Perfil

Gestión de la cuenta del avicultor: datos personales y de la granja, preferencias de alertas y cambio de contraseña. El módulo se apoya en los endpoints de `usuarios`.

- **Router:** `app/api/usuarios.py`
- **Tag:** `usuarios`
- **Autenticación:** todos los endpoints requieren JWT.
- **Servicio:** `app/services/auth_service.py`

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito | Documentado en |
|---|---|---|---|---|
| GET | `/api/usuarios/me` | Perfil, límites del plan e histórico. | 200 | [auth.md](auth.md#5-get-apiusuariosme) |
| PUT | `/api/usuarios/me` | Actualiza datos personales y de la granja. | 200 | este documento |
| PUT | `/api/usuarios/me/notificaciones` | Guarda preferencias de alertas. | 200 | este documento |
| PUT | `/api/usuarios/me/contrasena` | Cambia la contraseña. | 200 | este documento |

---

## 2. GET /api/usuarios/me

Devuelve el perfil del usuario autenticado con los límites/uso de su plan y el **histórico** de la cuenta. Detalle de campos base en [auth.md](auth.md#5-get-apiusuariosme).

### Campos añadidos por el módulo de perfil

| Campo | Tipo | Descripción |
|---|---|---|
| `nombre_granja` | string \| null | Nombre de la granja del avicultor. |
| `total_huevos_producidos` | int | Suma histórica de `produccion_diaria.total_huevos`. |
| `total_aves_gestionadas` | int | Suma de `camada.cantidad_inicial` de todas las camadas (histórico). |
| `notif_produccion_baja` | bool | Preferencia de alerta de producción baja. |
| `notif_stock_bajo` | bool | Preferencia de alerta de stock de insumo bajo. |
| `notif_vacunacion` | bool | Preferencia de recordatorio de vacunación. |
| `notif_resumen_semanal` | bool | Preferencia del resumen semanal. |

```json
{
  "id_usuario": 1,
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "nombre_granja": "Granja La Esperanza",
  "plan_suscripcion": "gratuito",
  "fecha_registro": "2026-01-05",
  "activo": true,
  "plan": "gratuito",
  "aves_max": 300,
  "clientes_max": 10,
  "ia_incluida": false,
  "aves_actuales": 85,
  "clientes_actuales": 3,
  "total_huevos_producidos": 4230,
  "total_aves_gestionadas": 118,
  "notif_produccion_baja": true,
  "notif_stock_bajo": true,
  "notif_vacunacion": false,
  "notif_resumen_semanal": true
}
```

---

## 3. PUT /api/usuarios/me

Actualiza el nombre, el correo, el teléfono y el nombre de la granja.

### Request — `PerfilUpdateRequest`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_completo` | string | Sí | 2 a 100 caracteres. |
| `correo_electronico` | string (email) | Sí | Formato válido y único. |
| `telefono` | string \| null | No | — |
| `nombre_granja` | string \| null | No | Máximo 100 caracteres. |
| `contrasena_actual` | string \| null | Condicional | **Obligatoria si cambia el correo**; debe coincidir con la contraseña vigente. |

```json
{
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana.nueva@example.com",
  "telefono": "3001234567",
  "nombre_granja": "Granja La Esperanza",
  "contrasena_actual": "Segura123!"
}
```

### Response 200

Devuelve el `PerfilResponse` completo con los cambios aplicados (mismos campos del apartado 2).

### Reglas de negocio

- Si el correo **no** cambia, no se exige `contrasena_actual`.
- Si el correo **cambia**, se valida la contraseña actual y la unicidad del nuevo correo.
- Por seguridad, el frontend **cierra la sesión** tras cambiar el correo para forzar un nuevo inicio de sesión.

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Cambio de correo sin contraseña o contraseña incorrecta | 401 | `La contraseña actual es incorrecta` |
| Nuevo correo ya registrado | 409 | `El correo ya está registrado` |
| Campos inválidos (nombre, email, longitudes) | 422 | Errores de Pydantic |

---

## 4. PUT /api/usuarios/me/notificaciones

Guarda las preferencias de alertas del avicultor. El **envío real** de las notificaciones (correo o teléfono) aún no está implementado: por ahora solo se persiste la preferencia.

### Request — `NotificacionesUpdateRequest`

| Campo | Tipo | Obligatorio |
|---|---|---|
| `notif_produccion_baja` | bool | Sí |
| `notif_stock_bajo` | bool | Sí |
| `notif_vacunacion` | bool | Sí |
| `notif_resumen_semanal` | bool | Sí |

```json
{
  "notif_produccion_baja": true,
  "notif_stock_bajo": true,
  "notif_vacunacion": false,
  "notif_resumen_semanal": true
}
```

### Response 200

Devuelve el `PerfilResponse` completo con las preferencias actualizadas.

### Validaciones y errores

| Validación | Código |
|---|---|
| Campos faltantes o tipo distinto de booleano | 422 |
| Sin token | 401 |

---

## 5. PUT /api/usuarios/me/contrasena

Cambia la contraseña del usuario autenticado validando la contraseña actual.

### Request — `CambiarContrasenaRequest`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `contrasena_actual` | string | Sí | Debe coincidir con la vigente. |
| `contrasena_nueva` | string | Sí | 8–72 caracteres; sin espacios; al menos 1 mayúscula, 1 número y 1 símbolo. |

```json
{
  "contrasena_actual": "Segura123!",
  "contrasena_nueva": "NuevaClave456!"
}
```

### Response 200

```json
{ "mensaje": "Contraseña actualizada correctamente" }
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Contraseña actual incorrecta | 401 | `La contraseña actual es incorrecta` |
| Contraseña nueva débil (longitud, espacios, mayúscula, número o símbolo) | 422 | Errores de validación |

---

## 6. Esquema de datos

La migración `i0d1e2f3a4b5_usuario_perfil_granja_notif` agrega a la tabla `usuario`:

| Columna | Tipo | Default |
|---|---|---|
| `nombre_granja` | TEXT (nullable) | `NULL` |
| `notif_produccion_baja` | BOOLEAN | `1` |
| `notif_stock_bajo` | BOOLEAN | `1` |
| `notif_vacunacion` | BOOLEAN | `0` |
| `notif_resumen_semanal` | BOOLEAN | `1` |

---

## 7. Frontend

- **Página:** `app/frontend/src/pages/Perfil.jsx` (dos columnas; en móvil las secciones cargan debajo).
- **Hook:** `app/frontend/src/hooks/usePerfil.js`
- **Servicio:** `app/frontend/src/services/perfilService.js`
- **Utilidades:** `app/frontend/src/utils/perfil.js`
- **Componentes:** `app/frontend/src/components/perfil/` (`PerfilUsuarioCard`, `PerfilHistoricoCard`, `PerfilMenu`, `EditarPerfilSeccion`, `NotificacionesSeccion`, `SeguridadSeccion`, `PlanSeccion`, `AyudaSeccion`, `Switch`).
- **Estilos:** `app/frontend/src/styles/perfil.css`

Secciones del submenú:

| Sección | Contenido |
|---|---|
| Editar Perfil (por defecto) | Datos personales y de la granja; el correo exige contraseña y cierra la sesión. |
| Notificaciones | Cuatro switches de alertas. |
| Seguridad | Cambio de contraseña (actual, nueva y confirmación). |
| Plan y Suscripción | Tarjeta informativa del plan y botón "Mejorar Plan" (sin acción). |
| Ayuda y Soporte | Formulario que abre el correo a `ayuda@eggchecker.click` con asunto y mensaje precargados, y PQR frecuentes desplegables. |

---

## 8. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 401 | Token ausente/inválido, o contraseña actual incorrecta. |
| 409 | Nuevo correo ya registrado. |
| 422 | Validación de Pydantic (campos, longitudes, fortaleza de contraseña). |
