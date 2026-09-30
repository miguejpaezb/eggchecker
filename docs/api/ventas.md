# Ventas

Servicios de venta: consulta del stock vendible y gestión de pedidos (creación con descuento de stock, edición, cambio de estado, cancelación y eliminación).

- **Router:** `app/api/ventas.py`
- **Tag:** `ventas`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/ventas/stock` | Stock vendible por tipo (excluye `No_apto`). | 200 |
| GET | `/api/ventas/pedidos` | Lista pedidos. | 200 |
| GET | `/api/ventas/pedidos/{id_pedido}` | Detalle de un pedido. | 200 |
| POST | `/api/ventas/pedidos` | Crea un pedido y descuenta stock. | 201 |
| PATCH | `/api/ventas/pedidos/{id_pedido}` | Edita un pedido pendiente reajustando stock. | 200 |
| PATCH | `/api/ventas/pedidos/{id_pedido}/estado` | Avanza el estado del pedido. | 200 |
| POST | `/api/ventas/pedidos/{id_pedido}/cancelar` | Cancela y repone el stock. | 200 |
| POST | `/api/ventas/pedidos/{id_pedido}/eliminar` | Elimina con contraseña y repone stock. | 204 |

---

## 2. Reglas de negocio

- Los huevos **`No_apto` no se pueden vender**.
- No se puede repetir el mismo tipo de huevo en un pedido.
- Se valida el **stock disponible por tipo** antes de descontar.
- `valor_total = Σ (precio_unitario × cantidad)` (calculado en el servidor).
- Estados del pedido: **`pendiente` → `enviado` → `recibido`**, más **`cancelado`**.
  - Solo se editan pedidos **pendientes**.
  - Cancelar repone el stock de las líneas.
- El `fecha_ultima_compra` del cliente se actualiza al registrar el pedido.
- Eliminar exige la **contraseña** del usuario y repone el stock.

---

## 3. GET /api/ventas/stock

Devuelve el stock vendible (excluye `No_apto`). Response 200 — `StockResponse`:

```json
{
  "total_disponible": 700,
  "por_tipo": [
    { "id_tipo": 1, "nombre_tipo": "AA", "cantidad_actual": 380, "valor_unidad": "500.00" },
    { "id_tipo": 2, "nombre_tipo": "A", "cantidad_actual": 320, "valor_unidad": "450.00" }
  ]
}
```

---

## 4. POST /api/ventas/pedidos

### Request — `PedidoCreate`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `id_cliente` | int | Sí | Debe existir, ser del usuario y estar activo. |
| `detalles` | lista | Sí | Al menos 1 línea; no se repite `id_tipo`. |

**Línea** — `PedidoDetalleCreate`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `id_tipo` | int | Sí | Debe existir y no ser `No_apto`. |
| `cantidad` | int | Sí | `> 0`. |
| `precio_unitario` | Decimal | Sí | `> 0`. |

```json
{
  "id_cliente": 1,
  "detalles": [
    { "id_tipo": 1, "cantidad": 30, "precio_unitario": 500 },
    { "id_tipo": 2, "cantidad": 20, "precio_unitario": 450 }
  ]
}
```

### Response 201 — `PedidoResponse`

```json
{
  "id_pedido": 1,
  "id_cliente": 1,
  "id_usuario": 1,
  "cliente_nombre": "Tienda El Huevo",
  "cliente_direccion": "Cra 5 #10-20",
  "fecha_pedido": "2026-09-25",
  "estado_pedido": "pendiente",
  "valor_total": "24000.00",
  "unidades_totales": 50,
  "detalles": [
    { "id_detalle_pedido": 1, "id_tipo": 1, "nombre_tipo": "AA", "cantidad": 30, "precio_unitario": "500.00", "subtotal": "15000.00" },
    { "id_detalle_pedido": 2, "id_tipo": 2, "nombre_tipo": "A", "cantidad": 20, "precio_unitario": "450.00", "subtotal": "9000.00" }
  ]
}
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Cliente inexistente o de otro usuario | 404 | `Cliente no encontrado` |
| Cliente suspendido | 400 | `El cliente está suspendido; actívalo para registrar la venta` |
| Tipo de huevo inexistente | 404 | `Tipo de huevo no encontrado` |
| Tipo `No_apto` | 400 | `Los huevos No_apto no se pueden vender` |
| Tipo repetido en el pedido | 400 | `No se puede repetir el mismo tipo de huevo en un pedido` |
| Stock insuficiente | 400 | `Stock insuficiente de {tipo}: disponible {n}` |
| Fallo de integridad al crear | 409 | `No se pudo registrar el pedido` |

```bash
curl -X POST http://127.0.0.1:8000/api/ventas/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id_cliente":1,"detalles":[{"id_tipo":1,"cantidad":30,"precio_unitario":500}]}'
```

---

## 5. Editar, cambiar estado, cancelar y eliminar

### PATCH /api/ventas/pedidos/{id_pedido}

Request — `PedidoUpdate`: `id_cliente` y/o `detalles` (mínimo 1 línea) opcionales. Reajusta el stock por la diferencia.

| Validación | Código | `detail` |
|---|---|---|
| Pedido inexistente | 404 | `Pedido no encontrado` |
| Pedido no pendiente | 400 | `Solo se pueden editar pedidos pendientes` |

### PATCH /api/ventas/pedidos/{id_pedido}/estado

Request — `CambiarEstadoRequest`:

| Campo | Tipo | Obligatorio | Valores |
|---|---|---|---|
| `estado` | string | Sí | `enviado` o `recibido`. |

Transiciones válidas: `enviado ← pendiente`; `recibido ← pendiente | enviado`.

| Validación | Código | `detail` |
|---|---|---|
| Transición inválida | 400 | `No se puede pasar el pedido a '{estado}'` |

### POST /api/ventas/pedidos/{id_pedido}/cancelar

Permitido solo si el pedido está `pendiente` o `enviado`; repone el stock y deja el estado en `cancelado`.

| Validación | Código | `detail` |
|---|---|---|
| Estado no cancelable | 400 | `Solo se pueden cancelar pedidos pendientes o en camino` |

### POST /api/ventas/pedidos/{id_pedido}/eliminar

Request — `PedidoEliminarRequest`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `contrasena` | string | Sí | Contraseña del usuario autenticado. |

| Validación | Código | `detail` |
|---|---|---|
| Contraseña incorrecta | 401 | `Contraseña incorrecta` |
| Estado no eliminable | 400 | `No se puede eliminar un pedido en este estado` |

---

## 6. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Cliente suspendido, `No_apto`, tipo repetido, stock insuficiente, estado no operable. |
| 401 | Token inválido o contraseña incorrecta al eliminar. |
| 404 | Cliente, tipo de huevo o pedido no encontrado. |
| 409 | Fallo de integridad al crear el pedido. |
| 422 | Validación Pydantic (cantidades, precios, líneas). |
