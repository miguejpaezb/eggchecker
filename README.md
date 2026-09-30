<div align="center">

<img src="docs/resources/logo-eggchecker.png" alt="EggChecker" width="140" />

# EggChecker

**Gestión inteligente de producción avícola**

Aplicación web para avicultores que centraliza la gestión de camadas, inventario de insumos, recolección diaria de producción, ventas y pedidos — con un módulo de inteligencia artificial para el análisis de imágenes de huevos.

</div>

---

## ✨ Características

- **Autenticación** — registro, inicio de sesión con JWT, recuperación de contraseña y perfil con límites por plan de suscripción.
- **Organización** — camadas de postura, inventario de insumos y seguimiento de movimientos.
- **Producción** — recolección diaria de huevos clasificados por tipo (AA, A, B, No apto) y acumulados.
- **Ventas** — clientes, pedidos y detalle de pedidos con descuento de stock.
- **Reportes** — consolidado de rentabilidad por período con exportación a PDF.
- **Notificaciones** — alertas de stock de insumos y recordatorios de decisión de camada.
- **Inteligencia** — módulo premium que analiza imágenes de huevos con IA (pendiente).
- **API REST** — backend reutilizable por aplicaciones web y móviles.

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Python 3.11 · FastAPI · SQLAlchemy · Pydantic · Uvicorn · JWT + bcrypt · ReportLab |
| Base de datos | MySQL 8 (producción) · SQLite (desarrollo) · Alembic (migraciones) |
| Frontend | React 19 · React Router · JavaScript (ES6+) · Vite · Bootstrap 5 |
| Calidad de código | Ruff + Black + pytest (Python) · ESLint (Airbnb) + Prettier (React) |
| Móvil (fase final) | Kotlin + Jetpack Compose (consume la misma API) |
| Control de versiones | Git + GitHub (GitHub Flow) |

## 🗃️ Base de datos

El modelo relacional se materializa con **16 tablas** y **3 vistas**:

- **Autenticación** — `usuario`, `token_recuperacion`.
- **Organización** — `camada`, `evento_sanitario`, `categoria_insumo`, `insumo`, `movimiento_insumo`.
- **Producción** — `tipo_huevo`, `produccion_diaria`, `produccion_detalle`, `stock_produccion`.
- **Ventas** — `cliente`, `pedido`, `detalle_pedido`.
- **Notificaciones** — `notificacion`.
- **IA** — `analisis_ia` (tabla creada; módulo pendiente).
- **Vistas** — `v_produccion_detallada`, `v_alertas_insumo`, `v_pedidos_pendientes`.

En desarrollo y QA la base se crea y se puebla con datos de prueba en SQLite mediante el script `cargar_seed.py` (idempotente). En producción se usa MySQL 8 gestionado con migraciones Alembic.

## 🚀 Primeros pasos

### Backend

```bash
cd app/backend
python -m venv .venv
.venv\Scripts\Activate.ps1        # Windows
source .venv/bin/activate         # macOS/Linux
pip install -r requirements.txt
python scripts/cargar_seed.py     # crea eggchecker.db con esquema + datos de prueba
uvicorn app.main:app --reload     # http://127.0.0.1:8000/docs
```

### Frontend

```bash
cd app/frontend
npm install
npm run dev                       # http://localhost:5173
```

### Pruebas y calidad

```bash
# Backend (desde app/backend)
pytest
ruff check app/ scripts/ tests/
black --check app/ scripts/ tests/

# Frontend (desde app/frontend)
npm run lint                      # ESLint (Airbnb) sobre src/
npm run format:check              # Prettier sobre src/
npm run build
```

## 🗺️ Estado de los módulos

| Módulo | Backend | Frontend | Detalle |
|---|---|---|---|
| Infraestructura | ✅ | ✅ | Monorepo, API base, base de datos con seed, linters y shell React |
| Autenticación y usuarios | ✅ | ✅ | Registro, login JWT, recuperación de contraseña y perfil con planes |
| Camadas | ✅ | ✅ | CRUD, mortalidad, edad y retiro estimado, ciclo de vida |
| Inventario | ✅ | ✅ | Categorías, insumos, movimientos, suspensión/descontinuación y alertas de umbral |
| Producción | ✅ | ✅ | Recolección diaria clasificada (AA, A, B, No apto), resumen y huevos disponibles |
| Clientes | ✅ | ✅ | CRUD, suspensión/activación y eliminación con confirmación de contraseña |
| Ventas (pedidos) | ✅ | ✅ | Stock vendible, pedidos con descuento de stock y cambio de estado |
| Reportes | ✅ | ✅ | Consolidado de rentabilidad por período y exportación a PDF |
| Notificaciones | ✅ | ✅ | Alertas de stock y recordatorios de decisión de camada |
| Inteligencia Premium (IA) | ⬜ | ⬜ | Análisis de imágenes de huevos con IA (tabla `analisis_ia` creada) |
| Dashboard | — | ⬜ | Indicadores generales (placeholder) |
| Perfil | — | ⬜ | Gestión de la cuenta y plan (placeholder) |
| App móvil Android | ⬜ | ⬜ | Kotlin + Jetpack Compose sobre la misma API REST |

> Leyenda: ✅ implementado · ⬜ pendiente · — no aplica (solo frontend).

## 📚 Documentación

Para detalles extensos y técnicos, consulta la documentación en [`docs/`](docs/):

- [Estándares de codificación](docs/Coding_Standards.md) — convenciones de Python, JavaScript/React y SQL.
- [Documentación de la API](docs/api/README.md) — guía general, autenticación, convenciones y matriz de validaciones.
- [Resumen de la API](docs/api/RESUMEN.md) — visión general de los 55 endpoints por módulo.
- Documentación detallada por módulo: [auth](docs/api/auth.md), [camadas](docs/api/camadas.md), [inventario](docs/api/inventario.md), [producción](docs/api/produccion.md), [clientes](docs/api/clientes.md), [ventas](docs/api/ventas.md), [reportes](docs/api/reportes.md), [notificaciones](docs/api/notificaciones.md).

---

<div align="center">

**EggChecker** · Análisis y desarrollo de software (3134556) · SENA · 2026

</div>
