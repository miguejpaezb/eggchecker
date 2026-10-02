# Guía para contribuir a EggChecker

¡Gracias por aportar al proyecto! Este documento explica **cómo trabajar con ramas y Pull Requests** y **cómo escribir mensajes de commit claros**, para que el historial del repositorio se mantenga ordenado y toda contribución pase por revisión.

---

## 1. Equipo y roles

| Rol | Integrante | Responsabilidad en el flujo |
|---|---|---|
| Coordinador | Miguel | Facilita el flujo, hace el **merge final** a `main` (squash) y publica versiones. |
| SME | Nicolas | Valida que lo desarrollado cumple los criterios de aceptación. |
| QA principal | Yuliana | Verifica la Definición de Hecho y prueba las ramas antes de aprobar. |
| Documentación y QA de módulos refactorizados | Brian | QA de los módulos migrados de **Java a Python**; además mantiene la documentación. |

> Todos somos desarrolladores y todos usamos el mismo flujo: **rama → commits → Pull Request → revisión → merge**.

---

## 2. Requisitos y puesta en marcha

### Backend

```bash
cd app/backend
python -m venv .venv
.venv\Scripts\Activate.ps1        # Windows
source .venv/bin/activate         # macOS/Linux
pip install -r requirements.txt
python scripts/cargar_seed.py     # crea eggchecker.db con datos de prueba
uvicorn app.main:app --reload     # http://127.0.0.1:8000/docs
```

### Frontend

```bash
cd app/frontend
npm install
npm run dev                       # http://localhost:5173
```

Más detalles de instalación en el [README](README.md).

---

## 3. Modelo de ramas (GitHub Flow)

- **`main`** es la única rama estable y de producción. Está protegida: **nadie hace commits directos**.
- Cada tarea vive en una **rama de corta duración** creada desde `main` actualizado.
- Una rama **no se reutiliza**: tarea nueva = rama nueva.

### 3.1 Nombres de rama

Formato: `prefijo/<descripcion-corta>` (en minúsculas, sin tildes, con guiones `-`).

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `feature/` | Nueva funcionalidad | `feature/modulo-produccion` |
| `fix/` | Corrección de errores | `fix/calculo-total-produccion` |
| `docs/` | Documentación | `docs/guia-contribucion` |
| `refactor/` | Migración/port a Python sin cambiar comportamiento | `refactor/modulo-inventario-fastapi` |
| `test/` | Solo pruebas | `test/crud-clientes` |
| `chore/` | Mantenimiento o configuración | `chore/configurar-alembic` |
| `hotfix/` | Corrección urgente sobre `main` | `hotfix/error-autenticacion` |

El prefijo de la rama coincide con el **tipo del commit principal** que contiene.

### 3.2 Ciclo de vida de una rama

```text
crear rama desde main -> desarrollar y commitear -> subir a GitHub -> PR -> revisión -> merge -> eliminar rama
```

```bash
git checkout main
git pull
git checkout -b feature/modulo-produccion
```

---

## 4. Mensajes de commit (Conventional Commits)

### 4.1 Formato

```text
<tipo>[alcance]: <descripción en imperativo>

[cuerpo opcional — por qué]

[pie opcional — Resuelve: PB-XX, BREAKING CHANGE:]
```

- **`tipo`**: obligatorio (ver tabla).
- **`[alcance]`**: opcional, entre paréntesis (`backend`, `frontend`, `api`, `db`, `ui`).
- **`descripción`**: obligatoria, en imperativo y minúscula inicial.

### 4.2 Tipos de commit

| Tipo | Cuándo se usa |
|---|---|
| `feat:` | Nueva funcionalidad |
| `fix:` | Corrección de un error |
| `docs:` | Documentación |
| `test:` | Agregar o corregir pruebas |
| `refactor:` | Reestructurar código sin cambiar comportamiento |
| `perf:` | Mejoras de rendimiento |
| `style:` | Solo formato (espacios, comas, lint) |
| `chore:` | Mantenimiento (dependencias, configuración) |
| `build:` | Sistema de build o empaquetado |
| `ci:` | Integración continua |
| `revert:` | Deshacer un commit anterior |

### 4.3 Reglas de redacción

- Imperativo ("agregar", "corregir", "actualizar"), no participio ("agregado").
- Minúscula inicial y **sin punto final**.
- Máximo ~72 caracteres; si falta contexto, usa el cuerpo.
- En español, coherente con el resto del proyecto.

### 4.4 Ejemplos

```text
feat(backend): agregar modelo ORM de camada
feat(api): crear endpoint POST /api/camadas
fix(backend): corregir validación de stock negativo en movimiento
docs: actualizar guía de contribución
test(backend): agregar pruebas del módulo de clientes
refactor(backend): migrar inventario a SQLAlchemy
```

| Incorrecto | Por qué | Correcto |
|---|---|---|
| `arreglé el bug` | Sin tipo ni imperativo | `fix: corregir cálculo del total` |
| `feat: agregado endpoint` | No está en imperativo | `feat: agregar endpoint POST /api/camadas` |
| `feat: Agregar endpoint.` | Mayúscula inicial y punto final | `feat: agregar endpoint POST /api/camadas` |
| `cambios varios` | No describe ni usa tipo | `refactor: extraer validación a un schema` |

### 4.5 Commits atómicos

- **Un commit = un cambio lógico.** No mezcles funcionalidades distintas.
- Haz commits **frecuentes y pequeños**.
- Usa `git add` por archivos, no siempre `git add .`.

```text
# Mal
feat: agregar camadas y corregir login

# Bien
feat(api): crear endpoint POST /api/camadas
fix(backend): corregir validación del token en login
```

---

## 5. Pull Requests

### 5.1 Flujo

1. Crea la rama desde `main` actualizado y trabaja con commits atómicos.
2. Sube la rama: `git push -u origin feature/modulo-produccion` (la primera vez).
3. Abre la Pull Request **hacia `main`** con la plantilla de abajo.
4. Solicita **1 aprobación** de un integrante distinto del autor (ver política de la sección 5.2).
5. Corrige lo solicitado **en la misma rama** y vuelve a hacer `push` (la PR se actualiza sola).
6. Con la aprobación, el **coordinador** integra con **Squash and merge** y se elimina la rama.

### 5.2 Política de aprobación

Toda Pull Request requiere **1 aprobación de un integrante distinto del autor**. El merge (**Squash and merge**) siempre lo realiza **Miguel**, como coordinador del proyecto.

| Autor | Aprueba |
|---|---|
| Miguel | Yuliana o Brian |
| Yuliana | Miguel |
| Brian | Yuliana o Miguel |
| Nicolas | Miguel o Brian |

### 5.3 Plantilla de descripción de PR

```markdown
## Descripción
<!-- Qué se hizo y por qué. -->

## Tarea del backlog
<!-- Ej. PB-07: registrar camada -->

## Cómo probarlo
<!-- Pasos concretos para que QA valide. -->

## Criterios de aceptación
<!-- Lista de lo que debe cumplir. -->

## Checklist (DoD)
- [ ] Código funcional y siguiendo los estándares de codificación
- [ ] Probado localmente (ruff/black, eslint/prettier)
- [ ] Sin romper funcionalidades existentes
- [ ] Documentación actualizada (si aplica)
```

### 5.4 Estrategia de merge

Se usa **Squash and merge**: todos los commits de la rama se compactan en uno sobre `main`, manteniendo el historial limpio.

```bash
# Después del merge, en local
git checkout main
git pull
git branch -d feature/modulo-produccion
```

---

## 6. Verificaciones antes de subir

```bash
# Backend (desde app/backend)
ruff check app/ scripts/ tests/
black --check app/ scripts/ tests/
pytest

# Frontend (desde app/frontend)
npm run lint
npm run format:check
npm run build
```

### Script de humo del CRUD

Verificación rápida del CRUD del módulo de clientes sobre una base temporal:

```bash
# Desde app/backend
python -m scripts.smoke_crud
```

---

## 7. Reglas de oro

1. **Nunca** commits directos a `main`. Siempre por Pull Request.
2. Rama nueva por cada tarea; nombres `feature/`, `fix/`, `docs/`, etc.
3. Commits con formato `tipo: descripción en imperativo`, minúscula y sin punto final.
4. Commits **atómicos y frecuentes**.
5. **Subir al remoto al final de cada jornada**.
6. Antes de empezar: `git checkout main` + `git pull`.
7. Nunca `git push --force` sobre ramas compartidas.
8. Nunca `git reset --hard` sobre trabajo no subido.
9. Nada se marca "Hecho" sin aprobación y merge a `main`.
10. **Nunca subir secretos** (`.env`, tokens, contraseñas). Verifica el `.gitignore`.

---

## 8. Documentación relacionada

- [README](README.md) — descripción del proyecto y puesta en marcha.
- [Documentación de la API](docs/api/README.md) — guía general, convenciones y validaciones.
- [Ejemplos de la API](docs/api/ejemplos.md) — solicitudes y respuestas de ejemplo.
- [Estándares de codificación](docs/Coding_Standards.md) — convenciones de Python, JavaScript/React y SQL.
