# Reportes

Reporte de rentabilidad consolidado del período y su exportación a PDF.

- **Router:** `app/api/reportes.py`
- **Tag:** `reportes`
- **Autenticación:** ambos endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/reportes/consolidado` | Reporte de rentabilidad consolidado. | 200 |
| GET | `/api/reportes/pdf` | Exporta el consolidado a PDF. | 200 (`application/pdf`) |

---

## 2. Parámetros (comunes a ambos endpoints)

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `desde` | date | No | Fecha inicial; por defecto el **primer día del mes actual**. |
| `hasta` | date | No | Fecha final; por defecto **hoy**. |
| `agrupacion` | string | No | `dia`, `semana` o `mes` (por defecto `dia`). |
| `camada` | int | No | Acota el reporte a una camada específica. |

---

## 3. GET /api/reportes/consolidado

### Response 200 — `ReporteConsolidadoResponse`

```json
{
  "periodo": { "desde": "2026-09-01", "hasta": "2026-09-25", "agrupacion": "dia" },
  "produccion": {
    "total_huevos": 1240,
    "cubetas_completas": 41,
    "huevos_no_aptos": 110,
    "promedio_diario": 49.6,
    "por_tipo": [ { "nombre_tipo": "AA", "cantidad": 500 } ],
    "cubetas_por_tipo": [ { "nombre_tipo": "AA", "cubetas": 16 } ],
    "por_camada": [ { "id_camada": 1, "nombre_camada": "La Esperanza", "total_huevos": 700 } ],
    "serie": [ { "periodo": "2026-09-25", "total_huevos": 42 } ]
  },
  "ventas": {
    "ingreso_total": "1250000.00",
    "unidades_totales": 900,
    "num_pedidos": 12,
    "ticket_promedio": "104166.67",
    "por_tipo": [],
    "top_clientes": [ { "id_cliente": 1, "nombre_cliente": "Tienda El Huevo", "ingreso_total": "500000.00", "num_pedidos": 5 } ],
    "serie": [ { "periodo": "2026-09-25", "ingreso_total": "80000.00" } ]
  },
  "gastos": { "total": "300000.00", "por_insumo": [] },
  "ganancia": { "valor": "950000.00", "porcentaje": "76.00" },
  "salud": { "gallinas_perdidas": 3, "causa_principal": "mortalidad", "vacunacion_al_dia": true },
  "alimento": { "kg_usados": "120.00", "promedio_diario": "4.80", "por_insumo": [] },
  "inventario": { "huevos": [], "valor_stock": "0.00", "insumos_bajo_umbral": 2 },
  "camada": null
}
```

### Secciones

| Sección | Contenido |
|---|---|
| `periodo` | Rango y agrupación usados. |
| `produccion` | Total de huevos, cubetas completas (`total / 30`), no aptos, promedio diario, por tipo/camada y serie temporal. |
| `ventas` | Ingreso total, unidades, número de pedidos, ticket promedio, top 5 clientes y serie (excluye pedidos cancelados). |
| `gastos` | Costo de insumos consumidos (`cantidad × costo unitario`). |
| `ganancia` | `valor = ingreso − gastos`; `porcentaje = valor / ingreso × 100`. |
| `salud` | Gallinas perdidas, causa principal y si la vacunación está al día. |
| `alimento` | Kg usados, promedio diario y desglose por insumo de categoría `alimento`. |
| `inventario` | Huevos en stock, valor del stock e insumos bajo umbral. |
| `camada` | Ficha de la camada si se filtró por una. |

### Agrupaciones de las series

| `agrupacion` | Formato del período |
|---|---|
| `dia` | `YYYY-MM-DD` |
| `semana` | `YYYY-Www` (semana ISO) |
| `mes` | `YYYY-MM` |

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| `desde` posterior a `hasta` | 400 | `La fecha inicial no puede ser posterior a la final` |
| Camada inexistente o de otro usuario | 404 | `Camada no encontrada` |

```bash
curl "http://127.0.0.1:8000/api/reportes/consolidado?desde=2026-09-01&hasta=2026-09-25&agrupacion=dia" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. GET /api/reportes/pdf

Genera el mismo consolidado y lo devuelve como PDF descargable (librería `reportlab`).

- **`Content-Type`:** `application/pdf`
- **`Content-Disposition`:** `attachment; filename="reporte_rentabilidad_{desde}_{hasta}[_{camada}].pdf"`

```bash
curl "http://127.0.0.1:8000/api/reportes/pdf?desde=2026-09-01&hasta=2026-09-25" \
  -H "Authorization: Bearer $TOKEN" -o reporte.pdf
```

---

## 5. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Rango de fechas inválido (`desde` > `hasta`). |
| 401 | Token ausente, inválido, expirado o usuario inactivo. |
| 404 | Camada no encontrada. |
| 422 | Parámetros con formato o valor inválido (`agrupacion`, fechas). |
