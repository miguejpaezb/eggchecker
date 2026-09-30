# Inventario

Gestión del inventario de insumos: catálogo de categorías, insumos, movimientos de entrada/salida (stock) y alertas de umbral mínimo.

- **Routers:** `app/api/categorias_insumo.py`, `app/api/insumos.py`
- **Tags:** `categorias-insumo`, `insumos`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

### 1.1 Categorías de insumo (catálogo global)

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/api/categorias-insumo` | Crea una categoría. | 201 |
| GET | `/api/categorias-insumo` | Lista el catálogo. | 200 |
| GET | `/api/categorias-insumo/{id_categoria}` | Obtiene una categoría. | 200 |
| PATCH | `/api/categorias-insumo/{id_categoria}` | Actualiza la categoría. | 200 |
| DELETE | `/api/categorias-insumo/{id_categoria}` | Elimina si no tiene insumos. | 204 |

### 1.2 Insumos y movimientos

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/api/insumos` | Crea un insumo. | 201 |
| GET | `/api/insumos` | Lista insumos (`?categoria=&activo=`). | 200 |
| GET | `/api/insumos/alertas` | Insumos bajo el umbral con su déficit. | 200 |
| GET | `/api/insumos/{id_insumo}` | Obtiene un insumo. | 200 |
| PATCH | `/api/insumos/{id_insumo}` | Edita nombre, unidad, umbral o costo. | 200 |
| POST | `/api/insumos/{id_insumo}/suspender` | Suspende (reversible). | 200 |
| POST | `/api/insumos/{id_insumo}/activar` | Reactiva un insumo suspendido. | 200 |
| POST | `/api/insumos/{id_insumo}/descontinuar` | Descontinúa (irreversible). | 200 |
| POST | `/api/insumos/{id_insumo}/movimientos` | Registra entrada/salida de stock. | 201 |
| GET | `/api/insumos/{id_insumo}/movimientos` | Historial de movimientos. | 200 |

> Los insumos pertenecen al usuario; las categorías son un **catálogo global** compartido.

---

## 2. Reglas de negocio

- El **stock nunca es negativo**: una salida mayor al stock disponible se rechaza.
- Al registrar una **entrada**, se recalcula el **costo promedio ponderado**:

  ```
  nuevo_costo = (stock × costo_actual + cantidad × costo_entrada) / (stock + cantidad)
  ```

  (redondeo a centavos, `HALF_UP`).
- La **salida** se valoriza al costo vigente del insumo.
- Un insumo **suspendido** no se puede manipular hasta reactivarlo.
- Un insumo **descontinuado** no se puede manipular ni reactivar (borrado lógico).
- Se genera una **alerta** cuando el stock activo cae bajo `umbral_minimo`; el `déficit = umbral_minimo − stock_actual`.

---

## 3. Categorías

### 3.1 POST /api/categorias-insumo

Request — `CategoriaCreate`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_categ` | string | Sí | 1 a 60 caracteres; único. |
| `descripcion` | string \| null | No | Hasta 200 caracteres. |

Response 201 — `CategoriaResponse`: `id_categoria`, `nombre_categ`, `descripcion`.

| Validación | Código | `detail` |
|---|---|---|
| Nombre duplicado | 409 | `La categoría ya existe` |
| Categoría inexistente | 404 | `Categoría no encontrada` |

```bash
curl -X POST http://127.0.0.1:8000/api/categorias-insumo \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre_categ":"Alimento","descripcion":"Concentrados y balanceados"}'
```

### 3.2 PATCH y DELETE

- `PATCH` permite `nombre_categ` y `descripcion` (ambos opcionales).
- `DELETE` falla si la categoría tiene insumos asociados.

| Validación | Código | `detail` |
|---|---|---|
| Nombre duplicado al editar | 409 | `La categoría ya existe` |
| Eliminar con insumos | 409 | `No se puede eliminar: tiene insumos asociados` |

---

## 4. Insumos

### 4.1 POST /api/insumos

Request — `InsumoCreate`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `id_categoria` | int | Sí | Debe existir. |
| `nombre_insumo` | string | Sí | 1 a 80 caracteres. |
| `unidad_medida` | string | Sí | 1 a 20 caracteres (ej. `kg`, `L`, `unidad`). |
| `stock_actual` | Decimal | Sí | `> 0`. |
| `umbral_minimo` | Decimal | Sí | `> 0`. |
| `costo_unitario` | Decimal | No | `>= 0` (por defecto `0.00`). |

Response 201 — `InsumoResponse`:

```json
{
  "id_insumo": 1,
  "id_usuario": 1,
  "id_categoria": 1,
  "nombre_insumo": "Concentrado ponedora",
  "unidad_medida": "kg",
  "stock_actual": "40.00",
  "umbral_minimo": "10.00",
  "costo_unitario": "2500.00",
  "activo": true,
  "descontinuado": false
}
```

| Validación | Código | `detail` |
|---|---|---|
| Categoría inexistente | 404 | `Categoría no encontrada` |

```bash
curl -X POST http://127.0.0.1:8000/api/insumos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id_categoria":1,"nombre_insumo":"Concentrado ponedora","unidad_medida":"kg","stock_actual":40,"umbral_minimo":10,"costo_unitario":2500}'
```

### 4.2 GET /api/insumos

| Parámetro | Tipo | Descripción |
|---|---|---|
| `categoria` | int | Filtra por categoría. |
| `activo` | bool | Si es `false`, incluye también los insumos desactivados. |

Por defecto solo se devuelven insumos **activos**; se excluyen los descontinuados.

### 4.3 PATCH /api/insumos/{id_insumo}

Request — `InsumoUpdate` (`extra="forbid"`): `nombre_insumo` (1–80), `unidad_medida` (1–20), `umbral_minimo` (`> 0`), `costo_unitario` (`>= 0`), todos opcionales.

| Validación | Código | `detail` |
|---|---|---|
| Descontinuado | 400 | `El insumo está descontinuado y no se puede manipular` |
| Suspendido | 400 | `El insumo está suspendido; actívalo para poder usarlo` |

### 4.4 Suspender / activar / descontinuar

| Endpoint | Efecto | Errores |
|---|---|---|
| `suspender` | `activo = false` (reversible). | 400 `No se puede suspender un insumo descontinuado` |
| `activar` | `activo = true`. | 400 `Un insumo descontinuado no se puede reactivar` |
| `descontinuar` | `descontinuado = true`, `activo = false` (irreversible). | 400 `El insumo ya está descontinuado` |

---

## 5. Movimientos de stock

### 5.1 POST /api/insumos/{id_insumo}/movimientos

Request — `MovimientoCreate`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `tipo_movimiento` | string | Sí | `entrada` o `salida`. |
| `cantidad` | Decimal | Sí | `> 0`. |
| `costo_unitario` | Decimal \| null | No | `>= 0` (solo aplica a entradas). |
| `observaciones` | string \| null | No | — |

Response 201 — `MovimientoResponse`:

```json
{
  "id_movimiento": 5,
  "id_insumo": 1,
  "tipo_movimiento": "salida",
  "cantidad": "20.00",
  "costo_unitario": "2500.00",
  "fecha_movimiento": "2026-09-25T11:00:00",
  "observaciones": "Consumo semanal",
  "stock_resultante": "20.00"
}
```

| Validación | Código | `detail` |
|---|---|---|
| Stock insuficiente en salida | 400 | `Stock insuficiente para registrar la salida` |
| Insumo suspendido/descontinuado | 400 | `El insumo está suspendido; actívalo para poder usarlo` / `El insumo está descontinuado y no se puede manipular` |
| Inconsistencia de stock (integridad) | 400 | `No se pudo registrar el movimiento: stock inconsistente` |

```bash
curl -X POST http://127.0.0.1:8000/api/insumos/1/movimientos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo_movimiento":"salida","cantidad":20,"observaciones":"Consumo semanal"}'
```

### 5.2 GET /api/insumos/{id_insumo}/movimientos

Devuelve el historial (`MovimientoHistorialResponse`) ordenado por fecha descendente (sin `stock_resultante`).

---

## 6. Alertas de umbral

### GET /api/insumos/alertas

Devuelve los insumos **activos** cuyo `stock_actual < umbral_minimo`, ordenados por déficit descendente.

Response 200 — `list[AlertaInsumoResponse]`:

```json
[
  {
    "id_insumo": 2,
    "categoria": "Sanidad",
    "nombre_insumo": "Virkon",
    "stock_actual": "3.00",
    "umbral_minimo": "10.00",
    "deficit": "7.00"
  }
]
```

---

## 7. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Stock insuficiente, insumo suspendido/descontinuado, inconsistencia. |
| 401 | Token ausente, inválido, expirado o usuario inactivo. |
| 404 | `Categoría no encontrada` / `Insumo no encontrado`. |
| 409 | Categoría duplicada o con insumos asociados. |
| 422 | Validación Pydantic (rangos, tipos, longitudes). |
