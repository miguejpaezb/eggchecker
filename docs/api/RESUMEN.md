# Documentación de la API — EggChecker

**Evidencia:** GA7-220501096-AA5-EV03 — Diseño y desarrollo de servicios web
**Proyecto:** EggChecker
**Autores:** Brian Gonzalo Suárez Acevedo, Miguel José Páez Barboza, Nicolas Sarmiento Bernal, Yuliana Jaramillo García
**Instructor:** Diego Fernando Henao Rojas
**Programa:** Análisis y Desarrollo de Software
**Grupo:** 3134556
**Institución:** SENA
**Versión:** 1.0
**Fecha:** 2026-09-25
**Repositorio:** https://github.com/miguejpaezb/eggchecker

---

## 1. Introducción

EggChecker es una aplicación web para avicultores que centraliza la gestión de camadas, inventario de insumos, producción diaria, ventas y reportes. Su backend es una **API REST** reutilizable por la web y por el futuro cliente móvil.

Esta API se construyó con **FastAPI** (Python) siguiendo el patrón de capas: **routers → services → models**, con **Pydantic** para la validación de datos y **SQLAlchemy** como ORM. Expone **55 endpoints** agrupados en **9 módulos funcionales**.

| Aspecto | Valor |
|---|---|
| Framework | FastAPI (Python 3.11+) |
| ORM | SQLAlchemy 2.0 |
| Validación | Pydantic |
| Base de datos | SQLite (dev) / MySQL 8 (producción) |
| Autenticación | JWT (HS256) + bcrypt |
| Servidor | Uvicorn |
| Documentación interactiva | Swagger UI en `/docs` |

---

## 2. Arquitectura

```
Cliente (React / móvil)
        │  HTTP JSON + JWT
        ▼
┌──────────────────────────────────────────┐
│  FastAPI (/api)                          │
│   routers  (api/*.py)   → rutas HTTP      │
│   services (*_service.py) → reglas negocio│
│   schemas  (schemas/*.py) → validación    │
│   models   (models/*.py) → ORM            │
└──────────────────────────────────────────┘
        ▼
   SQLite / MySQL
```

- **URL base:** `http://127.0.0.1:8000/api`
- **Swagger:** `http://127.0.0.1:8000/docs`
- **ReDoc:** `http://127.0.0.1:8000/redoc`
- **OpenAPI JSON:** `http://127.0.0.1:8000/openapi.json`

### Ejecución local

```bash
cd app/backend
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python scripts/cargar_seed.py
uvicorn app.main:app --reload
```

---

## 3. Autenticación

La API usa **JWT Bearer**. El flujo es:

1. `POST /api/auth/registro` crea la cuenta (contraseña cifrada con bcrypt).
2. `POST /api/auth/login` devuelve `access_token` (expira en 60 minutos).
3. Cada petición protegida envía `Authorization: Bearer <token>`.

Todos los endpoints requieren token **excepto** `health`, `auth/registro`, `auth/login` y `auth/recuperar`.

| Situación | Código | `detail` |
|---|---|---|
| Sin token | 401 | `No autenticado` |
| Token inválido/expirado | 401 | `Token inválido o expirado` |
| Credenciales incorrectas | 401 | `Credenciales inválidas` |

---

## 4. Módulos y servicios

| # | Módulo | Endpoints | Descripción |
|---|---|---|---|
| 1 | Health | 1 | Comprueba que la API está operativa. |
| 2 | Autenticación y usuarios | 4 | Registro, login, recuperación y perfil con límites del plan. |
| 3 | Camadas | 9 | CRUD, mortalidad, edad, retiro y ciclo de vida del lote. |
| 4 | Inventario | 15 | Categorías, insumos, movimientos de stock y alertas de umbral. |
| 5 | Producción | 5 | Recolección diaria por tipo de huevo, resumen y disponibles. |
| 6 | Clientes | 7 | CRUD, suspensión/activación y eliminación con contraseña. |
| 7 | Ventas | 8 | Stock vendible y gestión completa de pedidos. |
| 8 | Reportes | 2 | Consolidado de rentabilidad y exportación a PDF. |
| 9 | Notificaciones | 4 | Alertas de stock y recordatorios de camada. |
| | **Total** | **55** | |

### 4.1 Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/health` | Estado del servicio. |
| POST | `/api/auth/registro` | Registro de usuario. |
| POST | `/api/auth/login` | Inicio de sesión (JWT). |
| GET | `/api/usuarios/me` | Perfil y límites del plan. |
| POST | `/api/camadas` | Crear camada. |
| POST | `/api/camadas/{id}/mortalidad` | Registrar mortalidad. |
| GET | `/api/camadas/{id}` | Detalle con edad y retiro. |
| POST | `/api/insumos` | Crear insumo. |
| POST | `/api/insumos/{id}/movimientos` | Entrada/salida de stock. |
| GET | `/api/insumos/alertas` | Insumos bajo umbral. |
| POST | `/api/produccion` | Registrar recolección diaria. |
| GET | `/api/produccion/resumen` | Acumulados día/semana/mes. |
| GET | `/api/huevos/disponibles` | Inventario de huevos. |
| POST | `/api/clientes` | Crear cliente. |
| POST | `/api/ventas/pedidos` | Crear pedido (descuenta stock). |
| PATCH | `/api/ventas/pedidos/{id}/estado` | Avanzar estado del pedido. |
| GET | `/api/reportes/consolidado` | Reporte de rentabilidad. |
| GET | `/api/reportes/pdf` | Exportar reporte a PDF. |
| GET | `/api/notificaciones` | Listar notificaciones. |

---

## 5. Validaciones de verificación

La API valida en **tres niveles**:

| Nivel | Qué valida | Respuesta |
|---|---|---|
| **Pydantic** | Tipos, obligatorios, rangos (`gt`, `ge`), longitudes, `EmailStr`, `Literal` | 422 |
| **Negocio (services)** | Fechas, límites de plan, stock, estados, pertenencia del recurso | 400 / 404 / 409 |
| **Integridad (BD)** | `UNIQUE`, `CHECK`, `FOREIGN KEY` | 409 / 400 |

### 5.1 Validaciones destacadas

| Módulo | Validación | Respuesta |
|---|---|---|
| Auth | Contraseña: 8–72, sin espacios, 1 mayúscula, 1 número, 1 símbolo | 422 |
| Auth | Correo único | 409 `El correo ya está registrado` |
| Camadas | Fecha de ingreso solo hoy o ayer | 400 |
| Camadas | Límite de aves del plan | 400 `Límite de aves del plan alcanzado` |
| Camadas | Mortalidad no excede las aves actuales | 400 |
| Inventario | Stock nunca negativo | 400 `Stock insuficiente para registrar la salida` |
| Inventario | Categoría con insumos no se elimina | 409 |
| Producción | Solo fecha actual y camada en etapa (≥ 28 semanas) | 400 |
| Producción | Un registro por camada y fecha | 409 |
| Clientes | Límite de clientes del plan | 400 |
| Ventas | Stock suficiente y sin tipos `No_apto` ni repetidos | 400 |
| Reportes | `desde` ≤ `hasta` | 400 |
| Transversal | Aislamiento por usuario (datos de otro usuario) | 404 |

### 5.2 Códigos HTTP usados

| Código | Significado |
|---|---|
| 200 / 201 / 204 | Éxito: consulta, creación y acción sin cuerpo. |
| 400 | Regla de negocio incumplida. |
| 401 | Autenticación fallida o contraseña incorrecta al eliminar. |
| 404 | Recurso inexistente o de otro usuario. |
| 409 | Duplicado o conflicto de integridad. |
| 422 | Validación automática de Pydantic. |
| 500 | Fallo al enviar el correo de recuperación. |

---

## 6. Ejemplos de uso

### 6.1 Registro e inicio de sesión

```bash
BASE=http://127.0.0.1:8000/api

curl -X POST $BASE/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre_completo":"Ana Avicultora","correo_electronico":"ana@example.com","contrasena":"Segura123!","plan_suscripcion":"gratuito"}'

TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo_electronico":"ana@example.com","contrasena":"Segura123!"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
```

### 6.2 Crear una camada y registrar mortalidad

```bash
curl -X POST $BASE/camadas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre_camada":"La Esperanza","fecha_ingreso":"2026-09-25","cantidad_inicial":50}'

curl -X POST $BASE/camadas/1/mortalidad -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cantidad":3}'
```

### 6.3 Movimiento de inventario

```bash
curl -X POST $BASE/insumos/1/movimientos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo_movimiento":"salida","cantidad":20,"observaciones":"Consumo semanal"}'
```

### 6.4 Registrar pedido

```bash
curl -X POST $BASE/ventas/pedidos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id_cliente":1,"detalles":[{"id_tipo":1,"cantidad":30,"precio_unitario":500}]}'
```

### 6.5 Reporte de rentabilidad

```bash
curl "$BASE/reportes/consolidado?desde=2026-09-01&hasta=2026-09-25" -H "Authorization: Bearer $TOKEN"
```

---

## 7. Criterios de la evidencia cubiertos

| Criterio (25% c/u) | Cómo se cumple |
|---|---|
| Servicios según requerimientos | 55 endpoints en 9 módulos que cubren autenticación, camadas, inventario, producción, clientes, ventas, reportes y notificaciones. |
| API REST | Rutas con sustantivos, verbos HTTP (GET/POST/PATCH/DELETE), códigos de estado y JSON. |
| Validaciones de verificación | Validación en tres niveles (Pydantic, negocio e integridad) documentada por módulo. |
| Herramientas de versionamiento | Proyecto en Git/GitHub con GitHub Flow y Conventional Commits. |

---

## 8. Documentación detallada por módulo

Disponible en `docs/api/`:

- [Guía general](README.md)
- [auth.md](auth.md) — autenticación y usuarios
- [camadas.md](camadas.md) — camadas
- [inventario.md](inventario.md) — categorías, insumos y movimientos
- [produccion.md](produccion.md) — producción
- [clientes.md](clientes.md) — clientes
- [ventas.md](ventas.md) — stock y pedidos
- [reportes.md](reportes.md) — reportes
- [notificaciones.md](notificaciones.md) — notificaciones
