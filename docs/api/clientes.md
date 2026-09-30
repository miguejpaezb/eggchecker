# Clientes

Gestión de los clientes del avicultor: creación, consulta, edición, suspensión/activación y eliminación permanente con confirmación de contraseña.

- **Router:** `app/api/clientes.py`
- **Tag:** `clientes`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/api/clientes` | Crea un cliente. | 201 |
| GET | `/api/clientes` | Lista clientes (`?activo=false` incluye suspendidos). | 200 |
| GET | `/api/clientes/{id_cliente}` | Obtiene un cliente. | 200 |
| PATCH | `/api/clientes/{id_cliente}` | Edita nombre, teléfono o dirección. | 200 |
| POST | `/api/clientes/{id_cliente}/suspender` | Suspende (reversible). | 200 |
| POST | `/api/clientes/{id_cliente}/activar` | Reactiva un cliente suspendido. | 200 |
| POST | `/api/clientes/{id_cliente}/eliminar` | Elimina con confirmación de contraseña. | 204 |

---

## 2. Reglas de negocio

- Cada cliente pertenece al usuario autenticado.
- Se valida el **límite de clientes del plan** al crear (plan `gratuito` = 10).
- Por defecto solo se listan los clientes **activos**; `?activo=false` incluye los suspendidos.
- El campo `fecha_ultima_compra` lo actualiza el módulo de ventas al registrar un pedido.
- La eliminación es **permanente**, exige la **contraseña** del usuario y se bloquea si el cliente tiene pedidos asociados.

---

## 3. POST /api/clientes

### Request — `ClienteCreate`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_cliente` | string | Sí | 1 a 80 caracteres. |
| `telefono` | string \| null | No | Hasta 20 caracteres; solo dígitos (vacío → `null`). |
| `direccion` | string \| null | No | Hasta 200 caracteres. |

```json
{ "nombre_cliente": "Tienda El Huevo", "telefono": "3001234567", "direccion": "Cra 5 #10-20" }
```

### Response 201 — `ClienteResponse`

```json
{
  "id_cliente": 1,
  "id_usuario": 1,
  "nombre_cliente": "Tienda El Huevo",
  "telefono": "3001234567",
  "direccion": "Cra 5 #10-20",
  "fecha_ultima_compra": null,
  "activo": true
}
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Supera el límite de clientes del plan | 400 | `Límite de clientes del plan alcanzado` |
| Teléfono con caracteres no numéricos | 422 | `El teléfono solo puede contener números` |

```bash
curl -X POST http://127.0.0.1:8000/api/clientes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre_cliente":"Tienda El Huevo","telefono":"3001234567","direccion":"Cra 5 #10-20"}'
```

---

## 4. GET /api/clientes

| Parámetro | Tipo | Descripción |
|---|---|---|
| `activo` | bool | Si es `false`, incluye también los clientes suspendidos. |

Devuelve `list[ClienteResponse]` ordenada por nombre.

```bash
curl "http://127.0.0.1:8000/api/clientes?activo=false" -H "Authorization: Bearer $TOKEN"
```

---

## 5. PATCH /api/clientes/{id_cliente}

### Request — `ClienteUpdate` (`extra="forbid"`)

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_cliente` | string \| null | No | 1 a 80 caracteres. |
| `telefono` | string \| null | No | Hasta 20 caracteres; solo dígitos. |
| `direccion` | string \| null | No | Hasta 200 caracteres. |

| Validación | Código | `detail` |
|---|---|---|
| Cliente inexistente o de otro usuario | 404 | `Cliente no encontrado` |

---

## 6. Suspender y activar

| Endpoint | Efecto | Errores |
|---|---|---|
| `POST /clientes/{id}/suspender` | `activo = false` (reversible). | 404 `Cliente no encontrado` |
| `POST /clientes/{id}/activar` | `activo = true`. | 404 `Cliente no encontrado` |

> Un cliente suspendido no puede recibir pedidos nuevos (ver [ventas.md](ventas.md)).

---

## 7. POST /api/clientes/{id_cliente}/eliminar

### Request — `ClienteEliminarRequest`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `contrasena` | string | Sí | Contraseña del usuario autenticado. |

| Validación | Código | `detail` |
|---|---|---|
| Contraseña incorrecta | 401 | `Contraseña incorrecta` |
| Cliente inexistente | 404 | `Cliente no encontrado` |
| Cliente con pedidos asociados | 409 | `No se puede eliminar: el cliente tiene pedidos asociados` |

```bash
curl -X POST http://127.0.0.1:8000/api/clientes/1/eliminar \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"contrasena":"Segura123!"}'
```

---

## 8. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Límite de clientes del plan alcanzado. |
| 401 | Token inválido o contraseña incorrecta al eliminar. |
| 404 | `Cliente no encontrado`. |
| 409 | Cliente con pedidos asociados. |
| 422 | Validación Pydantic (teléfono, longitudes). |
