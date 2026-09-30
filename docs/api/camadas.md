# Camadas

Gestión de las camadas de aves (lote o grupo de gallinas ponedoras): creación, consulta, edición, mortalidad, avance de edad y retiro.

- **Router:** `app/api/camadas.py`
- **Tag:** `camadas`
- **Autenticación:** todos los endpoints requieren JWT.

---

## 1. Endpoints

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/api/camadas` | Crea una camada (inicia con 16 semanas de edad). | 201 |
| GET | `/api/camadas` | Lista las camadas del usuario (`?estado=activa`). | 200 |
| GET | `/api/camadas/{id_camada}` | Detalle con edad en días y retiro estimado. | 200 |
| PATCH | `/api/camadas/{id_camada}` | Edita nombre y, dentro de 24 h, cantidad inicial. | 200 |
| POST | `/api/camadas/{id_camada}/mortalidad` | Registra mortalidad y descuenta aves. | 200 |
| POST | `/api/camadas/{id_camada}/avanzar-semana` | Suma una semana de vida. | 200 |
| POST | `/api/camadas/{id_camada}/seguir-activa` | Pospone 7 días la decisión de retiro. | 200 |
| POST | `/api/camadas/{id_camada}/descartar` | Retira la camada (irreversible). | 200 |
| GET | `/api/camadas/{id_camada}/edad` | Edad en días y fecha de retiro estimada. | 200 |

---

## 2. Reglas de negocio

| Constante | Valor | Significado |
|---|---|---|
| Edad inicial | 16 semanas | Toda camada ingresa con 16 semanas. |
| Vida productiva | 504 días (72 semanas) | A partir de las 72 semanas se solicita decisión. |
| Etapa de producción | 28 semanas | La camada puede registrar producción desde las 28 semanas. |
| Decisión | 72 semanas | Se genera un aviso para decidir si la camada sigue activa. |
| Aviso | 7 días | Plazo que se pospone al "seguir activa". |
| Edición inicial | 24 horas | La cantidad inicial solo se edita dentro de las primeras 24 h. |

Además:

- Al crear la camada, `cantidad_actual = cantidad_inicial`.
- Se valida el **límite de aves del plan** del usuario (plan `gratuito` = 300).
- El estado de una camada es `activa` o `retirada`; una camada retirada no se reactiva.

---

## 3. POST /api/camadas

### Request — `CamadaCreate`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_camada` | string | Sí | 1 a 60 caracteres. |
| `fecha_ingreso` | date | Sí | Solo hoy o ayer. |
| `cantidad_inicial` | int | Sí | `> 0`. |
| `estado` | string | No | `activa` o `retirada` (por defecto `activa`). |

```json
{ "nombre_camada": "Camada La Esperanza", "fecha_ingreso": "2026-09-25", "cantidad_inicial": 50 }
```

### Response 201 — `CamadaResponse`

```json
{
  "id_camada": 1,
  "id_usuario": 1,
  "nombre_camada": "Camada La Esperanza",
  "fecha_ingreso": "2026-09-25",
  "cantidad_inicial": 50,
  "cantidad_actual": 50,
  "estado": "activa",
  "edad_semanas": 16,
  "fecha_proximo_aviso": null,
  "fecha_creacion": "2026-09-25T10:15:00",
  "requiere_decision": false,
  "puede_editar_inicial": true
}
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Fecha distinta de hoy/ayer | 400 | `La fecha de ingreso solo puede ser hoy o ayer` |
| Supera el límite de aves del plan | 400 | `Límite de aves del plan alcanzado` |
| `cantidad_inicial <= 0` o longitud inválida | 422 | validación Pydantic |

```bash
curl -X POST http://127.0.0.1:8000/api/camadas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre_camada":"Camada La Esperanza","fecha_ingreso":"2026-09-25","cantidad_inicial":50}'
```

---

## 4. GET /api/camadas

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `estado` | string | No | Filtra por `activa` o `retirada`. |

```bash
curl "http://127.0.0.1:8000/api/camadas?estado=activa" -H "Authorization: Bearer $TOKEN"
```

---

## 5. GET /api/camadas/{id_camada} y GET /api/camadas/{id_camada}/edad

El detalle (`CamadaDetalleResponse`) agrega a `CamadaResponse`:

| Campo | Descripción |
|---|---|
| `edad_dias` | `edad_semanas × 7`. |
| `fecha_retiro_estimada` | `hoy + max(0, 504 − edad_semanas × 7)` días. |

El endpoint `/edad` devuelve solo `edad_dias`, `fecha_ingreso` y `fecha_retiro_estimada`.

```bash
curl http://127.0.0.1:8000/api/camadas/1 -H "Authorization: Bearer $TOKEN"
curl http://127.0.0.1:8000/api/camadas/1/edad -H "Authorization: Bearer $TOKEN"
```

---

## 6. PATCH /api/camadas/{id_camada}

### Request — `CamadaUpdate` (`extra="forbid"`, es decir, no admite campos desconocidos)

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `nombre_camada` | string \| null | No | 1 a 60 caracteres. |
| `cantidad_inicial` | int \| null | No | `> 0`; solo dentro de 24 h desde `fecha_creacion`. |

```json
{ "nombre_camada": "Camada Renombrada" }
```

### Validaciones y errores

| Validación | Código | `detail` |
|---|---|---|
| Camada no activa | 400 | `Solo se pueden editar camadas activas` |
| Edición de cantidad inicial fuera de 24 h | 400 | `La cantidad inicial no se puede editar: la camada se registró hace más de 24 horas` |
| Ajuste deja `cantidad_actual` negativa | 400 | `El ajuste dejaría la cantidad actual en negativo` |
| Supera el límite de aves del plan | 400 | `Límite de aves del plan alcanzado` |
| Camada inexistente o de otro usuario | 404 | `Camada no encontrada` |

---

## 7. POST /api/camadas/{id_camada}/mortalidad

### Request — `MortalidadRequest`

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `cantidad` | int | Sí | `> 0`. |

```json
{ "cantidad": 3 }
```

Descuenta `cantidad` de `cantidad_actual` y crea un `EventoSanitario` de tipo `mortalidad`.

| Validación | Código | `detail` |
|---|---|---|
| Camada no activa | 400 | `Solo se puede registrar mortalidad en camadas activas` |
| Mortalidad mayor que la cantidad actual | 400 | `La mortalidad excede la cantidad actual` |
| `cantidad <= 0` | 422 | validación Pydantic |

---

## 8. POST /api/camadas/{id_camada}/avanzar-semana

Suma una semana a `edad_semanas`.

| Validación | Código | `detail` |
|---|---|---|
| Camada no activa | 400 | `Solo se puede avanzar semana en camadas activas` |

## 9. POST /api/camadas/{id_camada}/seguir-activa

Agenda `fecha_proximo_aviso = hoy + 7` días.

| Validación | Código | `detail` |
|---|---|---|
| Camada no activa | 400 | `Solo se puede continuar una camada activa` |
| La camada no requiere decisión | 400 | `La camada no requiere decisión` |

## 10. POST /api/camadas/{id_camada}/descartar

Cambia el estado a `retirada` (irreversible).

| Validación | Código | `detail` |
|---|---|---|
| Camada ya retirada | 400 | `La camada ya está retirada y no se puede reactivar` |

---

## 11. Códigos de error del módulo

| Código | Motivo |
|---|---|
| 400 | Reglas de negocio (fecha, límites, estados, mortalidad, ajustes). |
| 401 | Token ausente, inválido, expirado o usuario inactivo. |
| 404 | `Camada no encontrada` (inexistente o de otro usuario). |
| 422 | Validación Pydantic. |
