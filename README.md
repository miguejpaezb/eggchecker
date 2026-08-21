<div align="center">

<img src="docs/resources/logo-eggchecker.png" alt="EggChecker" width="140" />

# EggChecker

**Gestión inteligente de producción avícola**

Aplicación web para avicultores que centraliza la gestión de camadas, inventario de insumos, recolección diaria de producción, ventas y pedidos — con un módulo de inteligencia artificial para el análisis de imágenes de huevos.

</div>

---

## ✨ Características

- **Organización** — camadas de postura, inventario de insumos y seguimiento de movimientos.
- **Producción** — recolección diaria de huevos clasificados por tipo (AA, A, B, No apto).
- **Ventas** — clientes, pedidos y detalle de pedidos.
- **Inteligencia** — módulo premium que analiza imágenes de huevos con IA.
- **API REST** — backend reutilizable por aplicaciones web y móviles.

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Python 3 · FastAPI · SQLAlchemy · Pydantic · Uvicorn |
| Base de datos | MySQL 8 (producción) · SQLite (desarrollo) |
| Frontend | React · JavaScript (ES6+) |
| Control de versiones | Git + GitHub |

## 🚀 Primeros pasos

### Backend

```bash
cd app/backend
python -m venv .venv
.venv\Scripts\Activate.ps1        # Windows
source .venv/bin/activate         # macOS/Linux
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### Frontend

```bash
cd app/frontend
npm install
npm run dev
```

## 📚 Documentación

Para detalles extensos y técnicos, consulta la documentación en [`docs/`](docs/):

- [Estándares de codificación](docs/Coding_Standards.md) — convenciones de Python, JavaScript/React y SQL.

---

<div align="center">

**EggChecker** · Análisis y desarrollo de software (3134556) · SENA · 2026

</div>