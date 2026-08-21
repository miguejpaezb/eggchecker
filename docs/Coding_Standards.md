# EggChecker — Estándares de Codificación

**Versión:** 1.0 · **Año:** 2026

Este documento describe los estándares de codificación con los que se construye el software **EggChecker**: convenciones de nomenclatura, estructura de código, documentación y manejo de errores para Python (backend), JavaScript/React (frontend) y SQL (base de datos). Cualquier persona que ingrese al proyecto podrá revisar aquí la forma en que está escrito el código.

---

## Contenido

1. [Introducción](#1-introducción)
2. [Objetivos](#2-objetivos)
3. [Plataforma de desarrollo](#3-plataforma-de-desarrollo)
4. [Organización del código](#4-organización-del-código)
5. [Python — Backend](#5-python--backend)
6. [JavaScript/React — Frontend](#6-javascriptreact--frontend)
7. [SQL — Base de datos](#7-sql--base-de-datos)
8. [Comentarios y documentación](#8-comentarios-y-documentación)
9. [Indentación y formato (resumen)](#9-indentación-y-formato-resumen)

---

## 1. Introducción

EggChecker es una aplicación web para avicultores que permite gestionar la producción de huevos: organización de camadas, inventario de insumos, recolección diaria de producción, ventas y pedidos, y un módulo de inteligencia artificial para el análisis de imágenes.

El proyecto se desarrolla con una arquitectura orientada a servicios (API REST). El backend está construido en **Python** con **FastAPI**, el frontend en **React** con JavaScript (ES6+), y la base de datos de producción es **MySQL** (con **SQLite** para el entorno local de desarrollo).

Para garantizar uniformidad entre todos los desarrolladores, el código sigue convenciones establecidas por lenguaje: **PEP 8 / PEP 257** en Python, la guía de estilo **Airbnb** en JavaScript/React y **SQL ANSI** en la base de datos.

---

## 2. Objetivos

- Definir la nomenclatura de variables, funciones, clases, constantes, componentes y hooks del proyecto.
- Mantener una estructura de código organizada y uniforme en backend y frontend.
- Facilitar la legibilidad, el mantenimiento y la escalabilidad del sistema.
- Aplicar buenas prácticas de documentación y comentarios dentro del código.

---

## 3. Plataforma de desarrollo

| Capa | Tecnología |
|---|---|
| Backend | Python 3, FastAPI, SQLAlchemy, Pydantic, Uvicorn |
| Base de datos (producción) | MySQL 8 |
| Base de datos (desarrollo) | SQLite |
| Frontend | React, JavaScript (ES6+) |
| Control de versiones | Git + GitHub |
| Entorno de desarrollo | Visual Studio Code |

La arquitectura del sistema está orientada a servicios (API REST), de modo que el backend puede ser consumido por la aplicación web y, en el futuro, por aplicaciones móviles sin cambios en la lógica principal.

---

## 4. Organización del código

El proyecto mantiene una estructura por capas con responsabilidades específicas, evitando la duplicidad de funciones.

### Backend (FastAPI)

```text
app/
├── api/                 # Rutas y endpoints de la API (controllers)
│   ├── endpoints/
│   └── dependencies.py
├── core/                # Configuración general (settings, seguridad)
│   ├── config.py
│   └── security.py
├── models/              # Modelos ORM (SQLAlchemy)
├── schemas/             # Esquemas de validación (Pydantic)
├── services/            # Lógica de negocio
├── repositories/        # Acceso a datos
└── main.py              # Punto de entrada de la aplicación
```

### Frontend (React)

```text
src/
├── components/          # Componentes de interfaz de usuario
├── hooks/               # Hooks personalizados (useNombre)
├── pages/               # Vistas o páginas de la aplicación
├── services/            # Consumo de la API (fetch/axios)
├── context/             # Contexto global de la aplicación
├── utils/               # Funciones auxiliares
└── App.jsx              # Componente raíz
```

---

## 5. Python — Backend

El código Python se rige por **PEP 8** (estilo) y **PEP 257** (documentación).

### 5.1 Nomenclatura de variables

Nombres claros, descriptivos y relacionados con su función, en `snake_case`, iniciando con minúscula, sin abreviaturas innecesarias. Las variables booleanas expresan condiciones o estados.

```python
# Correcto
total_huevos = 42
nombre_camada = "Camada La Esperanza"
plan_premium = True

# Incorrecto
totalHuevos = 42      # usa camelCase
th = 42               # abreviatura innecesaria
premium = True        # no expresa claramente el estado
```

### 5.2 Nomenclatura de constantes

Las constantes se escriben en `UPPER_SNAKE_CASE` y se declaran en la parte superior del módulo.

```python
MINIMO_UMBRAL_INSUMO = 10.0
PRECIO_HUEVO_AA = 400.00
DIAS_MAXIMOS_ALERTA = 7
```

### 5.3 Declaración de clases

Las clases usan `CamelCase`, son sustantivos descriptivos y cumplen una única responsabilidad.

```python
class CamadaService:
    """Servicio de lógica de negocio para el módulo de camadas."""

    def crear_camada(self, datos: CamadaCreate) -> Camada:
        ...
```

### 5.4 Declaración de funciones y métodos

Las funciones y métodos usan `snake_case`, inician con verbos y describen la acción realizada. Cada función cumple una única función e incluye type hints en parámetros y retorno.

```python
def calcular_produccion_total(registros: list[ProduccionDiaria]) -> int:
    """Suma el total de huevos recolectados en una lista de registros."""
    return sum(registro.total_huevos for registro in registros)


def listar_camadas_activas(usuario_id: int) -> list[Camada]:
    """Retorna las camadas activas de un avicultor."""
    return camada_repository.find_activas_por_usuario(usuario_id)
```

### 5.5 Documentación (docstrings)

Las clases públicas y funciones importantes llevan docstring en estilo Google o en una sola línea, con comentarios claros y breves.

```python
def registrar_movimiento_insumo(
    id_insumo: int, tipo: str, cantidad: Decimal
) -> MovimientoInsumo:
    """Registra una entrada o salida de un insumo.

    Args:
        id_insumo: Identificador del insumo a mover.
        tipo: Tipo de movimiento ('entrada' o 'salida').
        cantidad: Cantidad registrada en el movimiento.

    Returns:
        MovimientoInsumo: El movimiento recién creado.
    """
    ...
```

### 5.6 Formato

- Indentación de 4 espacios, sin tabulaciones.
- Longitud de línea de 79 caracteres (recomendado 88).
- Dos líneas en blanco entre funciones y clases; una entre métodos.
- Archivos guardados en codificación UTF-8.

### 5.7 Manejo de errores

Uso de `try/except` solo cuando es necesario. La API lanza excepciones `HTTPException` de FastAPI con mensajes claros; los errores importantes se registran con el logger de la aplicación.

```python
from fastapi import HTTPException, status

class CamadaService:
    def obtener_camada(self, id_camada: int) -> Camada:
        camada = camada_repository.find_by_id(id_camada)
        if camada is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="La camada no existe o fue retirada",
            )
        return camada
```

### 5.8 Ejemplo aplicado: endpoint FastAPI

```python
from fastapi import APIRouter, Depends, HTTPException, status

from app.schemas.camada import CamadaCreate, CamadaRead
from app.services.camada_service import CamadaService
from app.api.dependencies import get_current_usuario

router = APIRouter(prefix="/camadas", tags=["camadas"])


@router.post("", response_model=CamadaRead, status_code=status.HTTP_201_CREATED)
def crear_camada(
    datos: CamadaCreate,
    service: CamadaService = Depends(CamadaService),
    usuario: Usuario = Depends(get_current_usuario),
) -> CamadaRead:
    """Crea una nueva camada para el avicultor autenticado."""
    camada = service.crear_camada(usuario.id_usuario, datos)
    return CamadaRead.from_orm(camada)
```

---

## 6. JavaScript/React — Frontend

El código del frontend se rige por la guía de estilo **Airbnb**, verificada con **ESLint** y formateada con **Prettier**.

### 6.1 Nomenclatura de componentes

Los componentes usan `PascalCase` y reflejan su funcionalidad. Ejemplos: `ListarCamadas`, `RegistrarProduccion`, `PanelInsumos`.

```jsx
// Correcto
const ListarCamadas = () => {
  // ...
};

// Incorrecto
const listarCamadas = () => {
  // ...
};
```

### 6.2 Nomenclatura de hooks

Los hooks personalizados inician con el prefijo `use` seguido de una palabra en `PascalCase`. Ejemplos: `useProduccionDiaria`, `useInsumos`, `useAutenticacion`.

```jsx
// Correcto
const useProduccionDiaria = (idCamada) => {
  const [produccion, setProduccion] = useState([]);
  // ...
  return { produccion, cargarProduccion };
};

// Incorrecto
const produccionDiaria = (idCamada) => {
  // ...
};
```

### 6.3 Nomenclatura de props y variables

Props y variables en `camelCase`, iniciando con minúscula. Las props de eventos inician con `on` (`onClick`, `onGuardar`). Las constantes se escriben en `UPPER_SNAKE_CASE` y los booleanos expresan estados o condiciones.

```jsx
// Correcto
const ListarCamadas = ({ nombreCamada, totalHuevos, onGuardar }) => {
  // ...
};

const PRECIO_HUEVO_AA = 400.00;

// Incorrecto
const ListarCamadas = ({ NombreCamada, TotalHuevos, onguardar }) => {
  // ...
};
```

### 6.4 Formato

- Indentación de 2 espacios.
- Una prop por línea cuando el componente tiene varias.
- Comillas simples para strings; punto y coma obligatorio al final de las sentencias.
- Archivos guardados en codificación UTF-8.

### 6.5 Manejo de errores

Al consumir la API se usan bloques `try/catch` y se muestran mensajes claros al usuario mediante componentes de estado. No se capturan errores de forma silenciosa.

```jsx
const useProduccionDiaria = (idCamada) => {
  const [produccion, setProduccion] = useState([]);
  const [error, setError] = useState(null);

  const cargarProduccion = async () => {
    try {
      const datos = await produccionService.listar(idCamada);
      setProduccion(datos);
    } catch (e) {
      setError("No fue posible cargar la producción diaria");
    }
  };

  useEffect(() => {
    cargarProduccion();
  }, [idCamada]);

  return { produccion, error, cargarProduccion };
};
```

### 6.6 Ejemplo aplicado: componente ListarCamadas

```jsx
import React from 'react';
import useCamadas from '../hooks/useCamadas';

const ListarCamadas = () => {
  const { camadas, cargando, error } = useCamadas();

  if (cargando) {
    return <p>Cargando camadas...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
    <ul>
      {camadas.map((camada) => (
        <li key={camada.idCamada}>
          <strong>{camada.nombreCamada}</strong> — {camada.cantidadActual} aves
        </li>
      ))}
    </ul>
  );
};

export default ListarCamadas;
```

---

## 7. SQL — Base de datos

Los scripts y consultas se rigen por el estándar **SQL ANSI**.

### 7.1 Nomenclatura

- Palabras reservadas en **MAYÚSCULA** (`SELECT`, `FROM`, `WHERE`, `INSERT`, `CREATE`).
- Tablas y columnas en `snake_case`.
- Restricciones con prefijos: `pk_`, `fk_`, `uq_`, `chk_`, `idx_`.
- Vistas con `v_`, procedimientos almacenados con `sp_`, triggers con `trg_`.
- Valores alfanuméricos entre comillas simples; consultas con indentación y campos alineados.

### 7.2 Ejemplo aplicado: consulta SELECT

```sql
SELECT
    c.nombre_camada,
    c.cantidad_actual,
    pd.fecha_recoleccion,
    pd.total_huevos
FROM   camada c
JOIN   produccion_diaria pd ON c.id_camada = pd.id_camada
WHERE  c.estado = 'activa'
ORDER BY pd.fecha_recoleccion DESC;
```

### 7.3 Ejemplo aplicado: creación de tabla

```sql
CREATE TABLE camada (
    id_camada        INT  NOT NULL AUTO_INCREMENT,
    id_usuario       INT  NOT NULL,
    nombre_camada    VARCHAR(60) NOT NULL,
    fecha_ingreso    DATE NOT NULL,
    cantidad_inicial INT  NOT NULL,
    cantidad_actual  INT  NOT NULL,
    estado           ENUM('activa','retirada') NOT NULL DEFAULT 'activa',
    CONSTRAINT pk_camada          PRIMARY KEY (id_camada),
    CONSTRAINT fk_camada_usuario  FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_camada_cant_ini CHECK (cantidad_inicial > 0),
    CONSTRAINT chk_camada_cant_act CHECK (cantidad_actual  >= 0)
) ENGINE = InnoDB;
```

### 7.4 Comentarios en SQL

- `--` para comentarios de una línea.
- `/* ... */` para comentarios de múltiples líneas.
- Se documenta el propósito de tablas y consultas complejas.

```sql
-- Lista las camadas activas con su producción total por fecha
/* Consulta utilizada en el módulo de producción
   para el reporte diario del avicultor. */
SELECT ...
```

---

## 8. Comentarios y documentación

Los comentarios se utilizan solo cuando es necesario explicar lógica compleja o información importante. Son claros, breves y sin redundancias, y la documentación se mantiene actualizada.

| Lenguaje | Documentación obligatoria | Estilo |
|---|---|---|
| Python | Docstrings en clases y funciones públicas | PEP 257 / estilo Google |
| JavaScript/React | JSDoc en funciones y hooks reutilizables | JSDoc |
| SQL | Comentarios `--` en tablas y consultas complejas | `--` y `/* */` |

---

## 9. Indentación y formato (resumen)

| Lenguaje | Indentación | Nomenclatura | Longitud de línea |
|---|---|---|---|
| Python | 4 espacios | `snake_case` / `CamelCase` | 79 (recomendado 88) |
| JavaScript/React | 2 espacios | `camelCase` / `PascalCase` | 100 |
| SQL | 4 espacios | palabras reservadas en mayúsculas | libre |

Todos los archivos se guardan en codificación UTF-8 y se evita el uso de tabulaciones.