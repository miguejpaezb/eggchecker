# Notificaciones

Avisos y recordatorios del sistema para el avicultor (alertas de stock y decisiones de camada).

- **Router:** `app/api/notificaciones.py`
- **Tag:** `notificaciones`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/notificaciones` | Lista las notificaciones (no leídas primero). | 200 |
| POST | `/api/notificaciones/leer-todas` | Marca todas como leídas. | 204 |
| POST | `/api/notificaciones/{id_notificacion}/leer` | Marca una como leída. | 200 |
| DELETE | `/api/notificaciones/{id_notificacion}` | Elimina (borrado lógico). | 204 |

---

## 2. Reglas de negocio

- Los avisos se **generan de forma perezosa** al consultarlos, según el estado actual de insumos y camadas:
  - **Insumos:** un aviso por insumo según su nivel de stock.
  - **Camadas:** aviso `camada_limite` cuando la camada alcanza las 72 semanas (requiere decisión).
- La eliminación es un **borrado lógico** (`eliminada = 1`): la notificación deja de mostrarse pero no se borra de la base.
- No se recrea un aviso de stock que el usuario ya descartó al mismo nivel.

### Tipos y severidad de aviso de stock

| Condición | Tipo de aviso |
|---|---|
| `stock_actual == 0` | `sin_stock` |
| `stock_actual < umbral_minimo` | `stock_bajo` |
| `stock_actual == umbral_minimo` | `stock_minimo` |
| `stock_actual > umbral_minimo` | Sin aviso (se cierra si existía) |

---

## 3. GET /api/notificaciones

### Response 200 — `list[NotificacionResponse]`

```json
[
  {
    "id_notificacion": 1,
    "id_camada": null,
    "id_insumo": 2,
    "tipo": "stock_bajo",
    "titulo": "Stock bajo de Virkon",
    "mensaje": "El insumo Virkon está por debajo del umbral mínimo.",
    "leida": false,
    "fecha_creacion": "2026-09-25T08:00:00"
  }
]
```

| Campo | Descripción |
|---|---|
| `id_camada` | Camada relacionada (o `null`). |
| `id_insumo` | Insumo relacionado (o `null`). |
| `tipo` | `camada_limite`, `stock_minimo`, `stock_bajo` o `sin_stock`. |
| `leida` | Si el usuario ya la leyó. |

```bash
curl http://127.0.0.1:8000/api/notificaciones -H "Authorization: Bearer $TOKEN"
```

---

## 4. Marcar como leídas

| Endpoint | Efecto | Respuesta |
|---|---|---|
| `POST /notificaciones/leer-todas` | Marca todas como leídas. | 204 |
| `POST /notificaciones/{id}/leer` | Marca una como leída. | 200 `NotificacionResponse` |

| Validación | Código | `detail` |
|---|---|---|
| Notificación inexistente o de otro usuario | 404 | `Notificación no encontrada` |

```bash
curl -X POST http://127.0.0.1:8000/api/notificaciones/leer-todas -H "Authorization: Bearer $TOKEN"
curl -X POST http://127.0.0.1:8000/api/notificaciones/1/leer -H "Authorization: Bearer $TOKEN"
```

---

## 5. DELETE /api/notificaciones/{id_notificacion}

Borrado lógico de la notificación (no aparece en el listado).

| Validación | Código | `detail` |
|---|---|---|
| Notificación inexistente o de otro usuario | 404 | `Notificación no encontrada` |

```bash
curl -X DELETE http://127.0.0.1:8000/api/notificaciones/1 -H "Authorization: Bearer $TOKEN"
```

---

## 6. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 401 | Token ausente, inválido, expirado o usuario inactivo. |
| 404 | `Notificación no encontrada`. |
