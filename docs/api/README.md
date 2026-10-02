# EggChecker API — Documentación de servicios web

API REST del proyecto **EggChecker**, construida con **FastAPI + SQLAlchemy + Pydantic**. Expone los servicios necesarios para la gestión avícola: autenticación, perfil, camadas, inventario de insumos, producción diaria, clientes, ventas, reportes, notificaciones y dashboard.

- **URL base (desarrollo):** `http://127.0.0.1:8000/api`
- **Swagger UI (interactivo):** `http://127.0.0.1:8000/docs`
- **ReDoc:** `http://127.0.0.1:8000/redoc`
- **OpenAPI JSON:** `http://127.0.0.1:8000/openapi.json`
- **Versión de la API:** `0.1.0`
- **Repositorio:** [github.com/miguejpaezb/eggchecker](https://github.com/miguejpaezb/eggchecker)

---

## 1. Cómo ejecutar la API

```bash
cd app/backend
python -m venv .venv
.venv\Scripts\Activate.ps1        # Windows
source .venv/bin/activate         # macOS/Linux
pip install -r requirements.txt
python scripts/cargar_seed.py     # crea la base SQLite con datos de prueba
uvicorn app.main:app --reload     # http://127.0.0.1:8000/docs
```

- **Base de datos local/dev:** SQLite (`app/backend/eggchecker.db`).
- **Base de datos producción:** MySQL 8 gestionada con migraciones Alembic.
- **CORS:** se permite el frontend de desarrollo en `http://localhost:5173`.

---

## 2. Autenticación

La API usa **JWT (JSON Web Token)** con contraseñas cifradas con **bcrypt**.

1. El usuario se registra (`POST /api/auth/registro`) e inicia sesión (`POST /api/auth/login`).
2. El login devuelve un `access_token` (Bearer, expira en **60 minutos**).
3. En cada petición protegida se envía el header:

```http
Authorization: Bearer <access_token>
```

Todos los endpoints requieren autenticación **excepto** `GET /api/health`, `POST /api/auth/registro`, `POST /api/auth/login` y `POST /api/auth/recuperar`.

| Situación | Código | `detail` |
|---|---|---|
| Sin header `Authorization` | 401 | `No autenticado` |
| Token inválido o expirado | 401 | `Token inválido o expirado` |
| Usuario inexistente o inactivo | 401 | `No autenticado` |

---

## 3. Convenciones

| Convención | Detalle |
|---|---|
| Formato | JSON (`application/json`), UTF-8. |
| Prefijo | Todos los servicios cuelgan de `/api`. |
| Nombres | `snake_case` en campos y rutas. |
| Fechas | ISO 8601 (`YYYY-MM-DD`). |
| Dinero | Tipo `Decimal`, con dos decimales. |
| Identificadores | Enteros (`id_camada`, `id_insumo`, etc.). |
| Pertenencias | Cada recurso pertenece al usuario autenticado; no se puede acceder a datos de otro usuario. |
| Versionado | Ruta plana `/api` (sin `/v1`); versión declarada en el OpenAPI (`0.1.0`). |

---

## 4. Formato de errores

FastAPI devuelve los errores con la estructura estándar `{"detail": ...}`:

```json
{ "detail": "Stock insuficiente para registrar la salida" }
```

En errores de validación automática (**422**) `detail` es una lista de campos inválidos:

```json
{
  "detail": [
    {
      "type": "greater_than",
      "loc": ["body", "cantidad"],
      "msg": "Input should be greater than 0"
    }
  ]
}
```

### 4.1 Matriz de códigos HTTP

| Código | Significado | Cuándo ocurre |
|---|---|---|
| 200 | OK | Consulta o actualización exitosa. |
| 201 | Created | Creación exitosa (registro, camada, insumo, pedido, producción). |
| 204 | No Content | Acción sin cuerpo de respuesta (marcar/eliminar). |
| 400 | Bad Request | Reglas de negocio: fechas, límites de plan, stock, estados no operables. |
| 401 | Unauthorized | Token ausente/inválido, credenciales inválidas o contraseña incorrecta al eliminar. |
| 403 | Forbidden | No se usa (la API no maneja roles). |
| 404 | Not Found | Recurso inexistente o de otro usuario. |
| 409 | Conflict | Duplicados o integridad (correo, categoría, producción duplicada, pedido). |
| 422 | Unprocessable Entity | Validación automática de Pydantic. |
| 500 | Internal Server Error | Fallo al enviar el correo de recuperación. |

---

## 5. Módulos y servicios documentados

| # | Módulo | Documento | Endpoints | Autenticación |
|---|---|---|---|---|
| 1 | Health (estado) | este README | 1 | No |
| 2 | Autenticación y usuarios | [auth.md](auth.md) | 4 | Mixto |
| 3 | Perfil | [perfil.md](perfil.md) | 3 | Sí |
| 4 | Camadas | [camadas.md](camadas.md) | 9 | Sí |
| 5 | Inventario (categorías, insumos, movimientos) | [inventario.md](inventario.md) | 15 | Sí |
| 6 | Producción | [produccion.md](produccion.md) | 5 | Sí |
| 7 | Clientes | [clientes.md](clientes.md) | 7 | Sí |
| 8 | Ventas (stock y pedidos) | [ventas.md](ventas.md) | 8 | Sí |
| 9 | Reportes | [reportes.md](reportes.md) | 2 | Sí |
| 10 | Notificaciones | [notificaciones.md](notificaciones.md) | 4 | Sí |
| 11 | Dashboard | [dashboard.md](dashboard.md) | 1 | Sí |
| | **Total** | | **59** | |

### 5.1 Health

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/health` | Comprueba que la API está operativa (sin tocar la base de datos). | 200 `{"status": "ok"}` |

```bash
curl http://127.0.0.1:8000/api/health
```

---

## 6. Matriz de validaciones de verificación

La API valida en **tres niveles** complementarios:

1. **Validación de entrada (Pydantic):** tipos, campos obligatorios y restricciones (`min_length`, `gt`, `ge`, `Literal`, `EmailStr`). Responde **422**.
2. **Validación de negocio (services):** reglas del dominio (fechas, límites de plan, stock, estados). Responde **400/404/409**.
3. **Validación de integridad (base de datos):** `UNIQUE`, `CHECK` y `FOREIGN KEY` como última barrera. Se traduce a **409** o **400**.

### 6.1 Validaciones transversales

| Validación | Nivel | Respuesta |
|---|---|---|
| Token JWT válido y vigente | Negocio | 401 `Token inválido o expirado` / `No autenticado` |
| Recurso pertenece al usuario (aislamiento multi-tenant) | Negocio | 404 (no se filtra la existencia de datos ajenos) |
| Tipos y campos obligatorios | Pydantic | 422 |
| Restricciones de rango (`gt=0`, `ge=0`, longitudes) | Pydantic | 422 |
| Valores enumerados (`Literal`) | Pydantic | 422 |
| Formato de correo (`EmailStr`) | Pydantic | 422 |

### 6.2 Validaciones por módulo

| Módulo | Validación destacada | Respuesta |
|---|---|---|
| Auth | Correo no duplicado | 409 `El correo ya está registrado` |
| Auth | Contraseña: sin espacios, 1 mayúscula, 1 número, 1 símbolo, 8–72 caracteres | 422 |
| Auth | Plan válido (`gratuito`/`premium`) | 422 |
| Perfil | Cambio de correo exige la contraseña actual | 401 `La contraseña actual es incorrecta` |
| Perfil | Nuevo correo no duplicado | 409 `El correo ya está registrado` |
| Perfil | Contraseña nueva: 8–72, sin espacios, 1 mayúscula, 1 número, 1 símbolo | 422 |
| Camadas | Fecha de ingreso solo hoy o ayer | 400 `La fecha de ingreso solo puede ser hoy o ayer` |
| Camadas | Límite de aves del plan | 400 `Límite de aves del plan alcanzado` |
| Camadas | Cantidad inicial editable solo dentro de 24 h | 400 `La cantidad inicial no se puede editar: la camada se registró hace más de 24 horas` |
| Camadas | Mortalidad no supera las aves actuales | 400 `La mortalidad excede la cantidad actual` |
| Inventario | Stock nunca negativo | 400 `Stock insuficiente para registrar la salida` |
| Inventario | Insumo suspendido/descontinuado no operable | 400 `El insumo está suspendido; actívalo para poder usarlo` |
| Inventario | Categoría sin insumos para eliminar | 409 `No se puede eliminar: tiene insumos asociados` |
| Producción | Solo fecha actual, camada en etapa (≥ 28 semanas) | 400 `Solo se puede registrar producción del día actual` / `La camada no está en etapa de producción` |
| Producción | Total no supera las aves de la camada | 400 `El total no puede superar las aves actuales de la camada` |
| Producción | Un registro por camada y fecha | 409 `Ya existe producción registrada para esta camada y fecha` |
| Clientes | Límite de clientes del plan | 400 `Límite de clientes del plan alcanzado` |
| Clientes | Teléfono solo numérico | 422 |
| Ventas | Stock suficiente por tipo | 400 `Stock insuficiente de {tipo}: disponible {n}` |
| Ventas | No vender huevos `No_apto` | 400 `Los huevos No_apto no se pueden vender` |
| Ventas | No repetir tipo en un pedido | 400 `No se puede repetir el mismo tipo de huevo en un pedido` |
| Ventas | Transiciones de estado válidas | 400 `No se puede pasar el pedido a '{estado}'` |
| Reportes | `desde` no posterior a `hasta` | 400 `La fecha inicial no puede ser posterior a la final` |

---

## 7. Ejemplo de flujo autenticado

```bash
BASE=http://127.0.0.1:8000/api

# 1) Registro
curl -X POST $BASE/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre_completo":"Ana Avicultora","correo_electronico":"ana@example.com","contrasena":"Segura123!","plan_suscripcion":"gratuito"}'

# 2) Login
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo_electronico":"ana@example.com","contrasena":"Segura123!"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

# 3) Perfil con límites del plan
curl $BASE/usuarios/me -H "Authorization: Bearer $TOKEN"
```

---

## 8. Índice

- [Autenticación y usuarios](auth.md)
- [Perfil](perfil.md)
- [Camadas](camadas.md)
- [Inventario](inventario.md)
- [Producción](produccion.md)
- [Clientes](clientes.md)
- [Ventas](ventas.md)
- [Reportes](reportes.md)
- [Notificaciones](notificaciones.md)
- [Dashboard](dashboard.md)
- [Ejemplos de solicitudes y respuestas](ejemplos.md)
- [Resumen general](RESUMEN.md)
