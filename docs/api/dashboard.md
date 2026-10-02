# Dashboard

Indicadores agregados de la granja del avicultor autenticado: KPI del día, resumen de los últimos siete días, alertas de insumos y pedidos recientes. Alimenta la vista principal del frontend.

- **Router:** `app/api/dashboard.py`
- **Tag:** `dashboard`
- **Autenticación:** requiere JWT.
- **Servicio:** `app/services/dashboard_service.py`

---

## 1. Endpoint

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/dashboard` | Indicadores agregados del usuario autenticado. | 200 |

No recibe parámetros: el usuario se resuelve del token y el período es fijo (hoy y los **6 días anteriores**).

```bash
curl http://127.0.0.1:8000/api/dashboard -H "Authorization: Bearer $TOKEN"
```

---

## 2. Response 200 — `DashboardResponse`

```json
{
  "produccion_hoy": 42,
  "variacion_produccion": "10.53",
  "aves_activas": 118,
  "pedidos_pendientes": 3,
  "alertas_count": 2,
  "semana": {
    "serie": [
      { "fecha": "2026-09-24", "etiqueta": "Lun", "total_huevos": 38 },
      { "fecha": "2026-09-25", "etiqueta": "Mar", "total_huevos": 42 }
    ],
    "total_huevos": 280,
    "mejor_dia": { "etiqueta": "Vie", "total_huevos": 50 },
    "valor_producido": "98000.00",
    "tasa_postura": "33.90",
    "mortalidad": 3
  },
  "alertas": [
    {
      "id_insumo": 3,
      "nombre_insumo": "Concentrado ponedora",
      "categoria": "alimento",
      "stock_actual": "12.00",
      "umbral_minimo": "25.00"
    }
  ],
  "pedidos_recientes": [
    {
      "id_pedido": 8,
      "cliente_nombre": "Tienda El Huevo",
      "descripcion": "60 AA + 30 A",
      "fecha_pedido": "2026-09-25",
      "estado_pedido": "pendiente"
    }
  ]
}
```

### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `produccion_hoy` | int | Huevos recolectados hoy. |
| `variacion_produccion` | decimal \| null | Variación porcentual frente a ayer; `null` si ayer no hubo producción. |
| `aves_activas` | int | Suma de `cantidad_actual` de las camadas con estado `activa`. |
| `pedidos_pendientes` | int | Pedidos en estado `pendiente` o `enviado`. |
| `alertas_count` | int | Cantidad de insumos bajo el umbral mínimo. |
| `semana` | objeto | Resumen de los últimos siete días (ver abajo). |
| `alertas` | lista | Insumos bajo umbral (`AlertaDashboard`). |
| `pedidos_recientes` | lista | Los **3** pedidos más recientes (`PedidoReciente`). |

### `semana` — `SemanaDashboard`

| Campo | Tipo | Descripción |
|---|---|---|
| `serie` | lista | Un `DiaProduccion` por día de la ventana, rellenando en cero los días sin registro. |
| `total_huevos` | int | Suma de la ventana. |
| `mejor_dia` | objeto \| null | Día de mayor recolección; `null` si no hubo producción. |
| `valor_producido` | decimal | Valoriza la producción con `stock_produccion.valor_unidad` por tipo. |
| `tasa_postura` | decimal | `total_huevos / (aves_activas × 7) × 100`. |
| `mortalidad` | int | Aves muertas registradas en eventos sanitarios de la ventana. |

`DiaProduccion`: `{ fecha, etiqueta, total_huevos }`. `MejorDia`: `{ etiqueta, total_huevos }`.
`AlertaDashboard`: `{ id_insumo, nombre_insumo, categoria, stock_actual, umbral_minimo }`.
`PedidoReciente`: `{ id_pedido, cliente_nombre, descripcion, fecha_pedido, estado_pedido }`.

---

## 3. Reglas de negocio

| Regla | Detalle |
|---|---|
| Ventana temporal | Hoy y los 6 días anteriores (7 días). |
| Variación | `(hoy − ayer) / ayer × 100`; `null` cuando `ayer = 0`. |
| Estados pendientes | `pendiente`, `enviado`. |
| Valor producido | Solo los tipos con precio de referencia configurado aportan; el resto aporta `0`. |
| Tasa de postura | Con `aves_activas = 0` devuelve `0`. |
| Mejor día | `null` si el total de la semana es `0`. |
| Top pedidos | Los 3 más recientes. |

---

## 4. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 401 | Token ausente, inválido, expirado o usuario inactivo. |

---

## 5. Frontend

- **Página:** `app/frontend/src/pages/Dashboard.jsx`
- **Hook:** `app/frontend/src/hooks/useDashboard.js`
- **Servicio:** `app/frontend/src/services/dashboardService.js`
- **Componentes:** `app/frontend/src/components/dashboard/` (`KpiCarrusel`, `KpiCard`, `ProduccionSemanalChart`, `ResumenSemanalCard`, `AlertasImportantesCard`, `PedidosRecientesCard`).
- **Estilos:** `app/frontend/src/styles/dashboard.css`

La vista muestra el carrusel de KPI (Producción Hoy, Aves activas, Pendientes, Alertas), la producción semanal con su resumen, las alertas de insumos y los pedidos recientes.
