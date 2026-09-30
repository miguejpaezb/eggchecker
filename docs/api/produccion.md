# Producción

Registro de la recolección diaria de huevos por camada, clasificada por tipo (AA, A, B, No_apto), junto con los acumulados y el inventario de huevos disponibles.

- **Router:** `app/api/produccion.py`
- **Tag:** `produccion`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/api/produccion` | Registra o actualiza la recolección del día. | 201 |
| GET | `/api/produccion` | Lista producción (`?camada=&fecha=`). | 200 |
| GET | `/api/produccion/resumen` | Acumulados de día, semana y mes. | 200 |
| GET | `/api/produccion/{id_produccion}` | Detalle de un registro. | 200 |
| GET | `/api/huevos/disponibles` | Inventario de huevos (producido − vendido). | 200 |

---

## 2. Reglas de negocio

- **1 cubeta = 30 huevos.** El campo `unidad` indica si las cantidades se envían en `unidad` o en `cubeta`.
- Solo se puede registrar producción de la **fecha actual**.
- La camada debe estar **activa** y tener al menos **28 semanas** (etapa de producción).
- El total debe ser **mayor que 0** y **no puede superar** la `cantidad_actual` de aves de la camada.
- Solo puede existir **un registro por camada y fecha**; si ya existe, se **actualiza** (upsert) y se aplica el **delta de stock** por la diferencia.

---

## 3. POST /api/produccion

### Request — `ProduccionCreate`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `id_camada` | int | Sí | Debe existir y estar en producción. |
| `fecha_recoleccion` | date | Sí | Debe ser la fecha actual. |
| `unidad` | string | No | `unidad` o `cubeta` (por defecto `unidad`). |
| `aa` | int | No | `>= 0` (por defecto 0). |
| `a` | int | No | `>= 0` (por defecto 0). |
| `b` | int | No | `>= 0` (por defecto 0). |
| `no_apto` | int | No | `>= 0` (por defecto 0). |
| `observaciones` | string \| null | No | — |

```json
{
  "id_camada": 1,
  "fecha_recoleccion": "2026-09-25",
  "unidad": "unidad",
  "aa": 20,
  "a": 15,
  "b": 5,
  "no_apto": 2
}
```

### Response 201 — `ProduccionConDetalleResponse`

```json
{
  "id_produccion": 1,
  "id_usuario": 1,
  "id_camada": 1,
  "fecha_recoleccion": "2026-09-25",
  "total_huevos": 42,
  "observaciones": null,
  "detalle": [
    { "id_detalle": 1, "id_produccion": 1, "id_tipo": 1, "nombre_tipo": "AA", "cantidad": 20 },
    { "id_detalle": 2, "id_produccion": 1, "id_tipo": 2, "nombre_tipo": "A", "cantidad": 15 },
    { "id_detalle": 3, "id_produccion": 1, "id_tipo": 3, "nombre_tipo": "B", "cantidad": 5 },
    { "id_detalle": 4, "id_produccion": 1, "id_tipo": 4, "nombre_tipo": "No_apto", "cantidad": 2 }
  ]
}
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Fecha distinta de hoy | 400 | `Solo se puede registrar producción del día actual` |
| Camada no encontrada / de otro usuario | 404 | `Camada no encontrada` |
| Camada fuera de etapa (< 28 semanas) | 400 | `La camada no está en etapa de producción` |
| Todos los tipos en 0 | 400 | `Debe registrar al menos un tipo de huevo` |
| Total mayor que las aves actuales | 400 | `El total no puede superar las aves actuales de la camada` |
| Duplicado por carrera (integridad) | 409 | `Ya existe producción registrada para esta camada y fecha` |

```bash
curl -X POST http://127.0.0.1:8000/api/produccion \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id_camada":1,"fecha_recoleccion":"2026-09-25","aa":20,"a":15,"b":5,"no_apto":2}'
```

---

## 4. GET /api/produccion

| Parámetro | Tipo | Descripción |
|---|---|---|
| `camada` | int | Filtra por camada. |
| `fecha` | date | Filtra por fecha de recolección. |

Devuelve `list[ProduccionResponse]` ordenada de la más reciente a la más antigua.

---

## 5. GET /api/produccion/resumen

Devuelve los acumulados del usuario. Response 200 — `ResumenResponse`:

```json
{
  "total_hoy": 42,
  "total_semana": 280,
  "total_mes": 1240,
  "por_tipo": { "AA": 500, "A": 380, "B": 250, "No_apto": 110 }
}
```

| Campo | Descripción |
|---|---|
| `total_hoy` | Huevos recolectados hoy. |
| `total_semana` | Huevos de los últimos 7 días. |
| `total_mes` | Huevos del mes en curso. |
| `por_tipo` | Desglose del mes (siempre incluye los 4 tipos). |

---

## 6. GET /api/produccion/{id_produccion}

Devuelve un registro concreto con su detalle (`ProduccionConDetalleResponse`).

| Validación | Código | `detail` |
|---|---|---|
| Registro inexistente o de otro usuario | 404 | `Producción no encontrada` |

---

## 7. GET /api/huevos/disponibles

Inventario de huevos del usuario: `disponible = max(producido − vendido, 0)`. El total vendido excluye los pedidos cancelados.

Response 200 — `DisponiblesResponse`:

```json
{
  "total_disponible": 830,
  "por_tipo": [
    { "id_tipo": 1, "nombre_tipo": "AA", "producido": 500, "vendido": 120, "disponible": 380 },
    { "id_tipo": 2, "nombre_tipo": "A", "producido": 380, "vendido": 60, "disponible": 320 }
  ]
}
```

---

## 8. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Fecha no actual, camada fuera de etapa, sin tipos, total excedido. |
| 401 | Token ausente, inválido, expirado o usuario inactivo. |
| 404 | Camada o producción no encontrada. |
| 409 | Registro duplicado por camada y fecha. |
| 422 | Validación Pydantic. |
