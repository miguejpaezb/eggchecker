# Autenticación y usuarios

Servicios de registro, inicio de sesión, recuperación de contraseña y consulta del perfil del usuario autenticado.

- **Router:** `app/api/auth.py`, `app/api/usuarios.py`
- **Tags:** `auth`, `usuarios`
- **Autenticación:** registro, login y recuperar son públicos; el perfil requiere JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito | Auth |
|---|---|---|---|---|
| POST | `/api/auth/registro` | Registra un avicultor nuevo. | 201 | No |
| POST | `/api/auth/login` | Inicia sesión y devuelve el token JWT. | 200 | No |
| POST | `/api/auth/recuperar` | Solicita el enlace de recuperación de contraseña. | 200 | No |
| GET | `/api/usuarios/me` | Perfil del usuario y límites/uso de su plan. | 200 | Sí |

---

## 2. POST /api/auth/registro

Crea la cuenta del avicultor. La contraseña se almacena cifrada con **bcrypt**.

### Request

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_completo` | string | Sí | 2 a 100 caracteres. |
| `correo_electronico` | string (email) | Sí | Formato de correo válido y único. |
| `contrasena` | string | Sí | 8 a 72 caracteres; sin espacios; al menos 1 mayúscula, 1 número y 1 símbolo. |
| `telefono` | string \| null | No | — |
| `plan_suscripcion` | string | No | `gratuito` o `premium` (por defecto `gratuito`). |

```json
{
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "contrasena": "Segura123!",
  "telefono": "3001234567",
  "plan_suscripcion": "gratuito"
}
```

### Response 201

`UsuarioResponse`:

```json
{
  "id_usuario": 1,
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "plan_suscripcion": "gratuito",
  "fecha_registro": "2026-09-25",
  "activo": true
}
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Contraseña con espacios | 422 | `La contraseña no puede contener espacios en blanco` |
| Sin mayúscula / número / símbolo | 422 | `Debe contener al menos una mayúscula` / `...un número` / `...un símbolo` |
| Plan desconocido | 422 | `Debe seleccionar un plan válido para poder registrarse` |
| Correo ya registrado | 409 | `El correo ya está registrado` |

```bash
curl -X POST http://127.0.0.1:8000/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre_completo":"Ana Avicultora","correo_electronico":"ana@example.com","contrasena":"Segura123!"}'
```

---

## 3. POST /api/auth/login

Valida las credenciales y devuelve un token JWT.

### Request

| Campo | Tipo | Obligatorio |
|---|---|---|
| `correo_electronico` | string (email) | Sí |
| `contrasena` | string | Sí |

### Response 200

`TokenResponse`:

```json
{ "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "token_type": "bearer" }
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Credenciales incorrectas o usuario inactivo | 401 | `Credenciales inválidas` |

```bash
curl -X POST http://127.0.0.1:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo_electronico":"ana@example.com","contrasena":"Segura123!"}'
```

---

## 4. POST /api/auth/recuperar

Genera un token de recuperación opaco (válido **30 minutos**) y envía el enlace al correo. En el entorno de desarrollo el envío es simulado y el token se persiste **hasheado** (SHA-256), nunca en claro.

### Request

| Campo | Tipo | Obligatorio |
|---|---|---|
| `correo_electronico` | string (email) | Sí |

### Response 200

```json
{ "mensaje": "Se ha enviado un enlace de recuperación a tu correo" }
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Correo no registrado | 404 | `No encontramos una cuenta con ese correo. ¿Deseas registrarte?` |
| Fallo al enviar el correo | 500 | `No se pudo enviar el correo de recuperación. Intenta de nuevo más tarde.` |

> **Pendiente:** el endpoint para confirmar el token y restablecer la contraseña aún no está implementado; el modelo `TokenRecuperacion` ya persiste el hash y la expiración.

```bash
curl -X POST http://127.0.0.1:8000/api/auth/recuperar \
  -H "Content-Type: application/json" \
  -d '{"correo_electronico":"ana@example.com"}'
```

---

## 5. GET /api/usuarios/me

Devuelve el perfil del usuario autenticado junto con los límites y el uso actual de su plan.

### Response 200

`PerfilResponse` (extiende `UsuarioResponse`):

```json
{
  "id_usuario": 1,
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "plan_suscripcion": "gratuito",
  "fecha_registro": "2026-09-25",
  "activo": true,
  "plan": "gratuito",
  "aves_max": 300,
  "clientes_max": 10,
  "ia_incluida": false,
  "aves_actuales": 85,
  "clientes_actuales": 3
}
```

| Campo | Descripción |
|---|---|
| `aves_max` | Máximo de aves permitido por el plan (`null` = ilimitado). |
| `clientes_max` | Máximo de clientes permitido por el plan (`null` = ilimitado). |
| `ia_incluida` | Si el plan habilita el módulo de IA. |
| `aves_actuales` | Suma de `cantidad_actual` de las camadas activas. |
| `clientes_actuales` | Total de clientes del usuario. |

```bash
curl http://127.0.0.1:8000/api/usuarios/me -H "Authorization: Bearer $TOKEN"
```

---

## 6. Planes de suscripción

Fuente: `app/core/plans.py`.

| Plan | Aves máx. | Clientes máx. | IA incluida |
|---|---|---|---|
| `gratuito` | 300 | 10 | No |
| `premium` | ilimitado | ilimitado | Sí |

> **Nota:** el modelo actual contempla dos planes (`gratuito`, `premium`). La ampliación a los cuatro planes del Anexo A (Productor, Avicultor Pro, Empresarial) está pendiente como evolución de esquema.

---

## 7. Seguridad

- Contraseñas cifradas con **bcrypt** (`app/core/security.py`).
- Token **JWT HS256** con expiración de 60 minutos.
- Token de recuperación opaco (`secrets.token_urlsafe(32)`) persistido como hash **SHA-256**.
- Protección de rutas con la dependencia `get_current_usuario` (`app/api/deps.py`).
