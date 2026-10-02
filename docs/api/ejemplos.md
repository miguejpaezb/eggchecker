# Ejemplos de solicitudes y respuestas

Colección de ejemplos de la API de EggChecker con la solicitud y la respuesta esperada de cada operación. Complementa la documentación por módulo de `docs/api/`.

- **URL base:** `http://127.0.0.1:8000/api`
- **Formato:** JSON (`application/json`)
- **Autenticación:** header `Authorization: Bearer <access_token>` en todos los endpoints, salvo registro, login, recuperación y health.
- Los ejemplos usan un `TOKEN` ya obtenido en el login.

> Para ver todos los endpoints interactivos: `http://127.0.0.1:8000/docs`.

---

## 1. Autenticación

### 1.1 Registro

**Solicitud**

```http
POST /api/auth/registro
Content-Type: application/json
```

```json
{
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "contrasena": "Segura123!",
  "telefono": "3001234567",
  "plan_suscripcion": "gratuito"
}
```

**Respuesta 201**

```json
{
  "id_usuario": 1,
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "nombre_granja": null,
  "plan_suscripcion": "gratuito",
  "fecha_registro": "2026-09-30",
  "activo": true
}
```

### 1.2 Inicio de sesión

**Solicitud**

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "correo_electronico": "ana@example.com",
  "contrasena": "Segura123!"
}
```

**Respuesta 200**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

---

## 2. CRUD de clientes

Ciclo completo sobre el módulo de clientes (crear, listar, obtener, actualizar y eliminar).

### 2.1 Crear

**Solicitud**

```http
POST /api/clientes
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "nombre_cliente": "Tienda El Huevo",
  "telefono": "3001234567",
  "direccion": "Calle 1 #2-3"
}
```

**Respuesta 201**

```json
{
  "id_cliente": 1,
  "id_usuario": 1,
  "nombre_cliente": "Tienda El Huevo",
  "telefono": "3001234567",
  "direccion": "Calle 1 #2-3",
  "fecha_ultima_compra": null,
  "activo": true
}
```

### 2.2 Listar

**Solicitud**

```http
GET /api/clientes
Authorization: Bearer <TOKEN>
```

**Respuesta 200**

```json
[
  {
    "id_cliente": 1,
    "id_usuario": 1,
    "nombre_cliente": "Tienda El Huevo",
    "telefono": "3001234567",
    "direccion": "Calle 1 #2-3",
    "fecha_ultima_compra": null,
    "activo": true
  }
]
```

> Por defecto se listan solo los clientes **activos**. Con `?activo=false` se incluyen también los suspendidos.

### 2.3 Obtener

**Solicitud**

```http
GET /api/clientes/1
Authorization: Bearer <TOKEN>
```

**Respuesta 200**

```json
{
  "id_cliente": 1,
  "id_usuario": 1,
  "nombre_cliente": "Tienda El Huevo",
  "telefono": "3001234567",
  "direccion": "Calle 1 #2-3",
  "fecha_ultima_compra": null,
  "activo": true
}
```

### 2.4 Actualizar

**Solicitud**

```http
PATCH /api/clientes/1
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "direccion": "Carrera 4 #5-6"
}
```

**Respuesta 200**

```json
{
  "id_cliente": 1,
  "id_usuario": 1,
  "nombre_cliente": "Tienda El Huevo",
  "telefono": "3001234567",
  "direccion": "Carrera 4 #5-6",
  "fecha_ultima_compra": null,
  "activo": true
}
```

### 2.5 Eliminar

La eliminación es **permanente** y exige la contraseña del usuario dueño.

**Solicitud**

```http
POST /api/clientes/1/eliminar
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "contrasena": "Segura123!"
}
```

**Respuesta 204** — sin contenido.

**Consulta posterior**

```http
GET /api/clientes/1
Authorization: Bearer <TOKEN>
```

**Respuesta 404**

```json
{
  "detail": "Cliente no encontrado"
}
```

> Este mismo ciclo CRUD se puede ejecutar de una sola vez con `python -m scripts.smoke_crud` desde `app/backend`.

---

## 3. Inventario: movimiento de stock

**Solicitud**

```http
POST /api/insumos/1/movimientos
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "tipo_movimiento": "salida",
  "cantidad": 20,
  "observaciones": "Consumo semanal"
}
```

**Respuesta 201**

```json
{
  "id_movimiento": 5,
  "id_insumo": 1,
  "tipo_movimiento": "salida",
  "cantidad": "20.00",
  "costo_unitario": "2500.00",
  "fecha_movimiento": "2026-09-30T11:00:00",
  "observaciones": "Consumo semanal",
  "stock_resultante": "20.00"
}
```

---

## 4. Producción diaria

> **Requisitos:** la camada debe estar **activa** y tener **≥ 28 semanas** (etapa de producción), y `fecha_recoleccion` debe ser la **fecha actual**.

**Solicitud**

```http
POST /api/produccion
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "id_camada": 1,
  "fecha_recoleccion": "2026-09-30",
  "unidad": "unidad",
  "aa": 20,
  "a": 15,
  "b": 5,
  "no_apto": 2
}
```

**Respuesta 201**

```json
{
  "id_produccion": 1,
  "id_usuario": 1,
  "id_camada": 1,
  "fecha_recoleccion": "2026-09-30",
  "total_huevos": 42,
  "observaciones": null,
  "detalle": [
    { "id_detalle": 1, "id_produccion": 1, "id_tipo": 1, "nombre_tipo": "AA", "cantidad": 20 },
    { "id_detalle": 2, "id_produccion": 1, "id_tipo": 2, "nombre_tipo": "A", "cantidad": 15 },
    { "id_detalle": 3, "id_produccion": 1, "id_tipo": 3, "nombre_tipo": "B", "cantidad": 5 },
    { "id_detalle": 4, "id_produccion": 1, "id_tipo": 4, "nombre_tipo": "No_apto", "cantidad": 2 }
  ]
}
```

---

## 5. Dashboard y perfil

### 5.1 Dashboard

**Solicitud**

```http
GET /api/dashboard
Authorization: Bearer <TOKEN>
```

**Respuesta 200** (resumida)

```json
{
  "produccion_hoy": 42,
  "variacion_produccion": "10.53",
  "aves_activas": 118,
  "pedidos_pendientes": 3,
  "alertas_count": 2,
  "semana": {
    "total_huevos": 280,
    "mejor_dia": { "etiqueta": "Vie", "total_huevos": 50 },
    "valor_producido": "98000.00",
    "tasa_postura": "33.90",
    "mortalidad": 3
  },
  "alertas": [],
  "pedidos_recientes": []
}
```

### 5.2 Actualizar perfil

**Solicitud**

```http
PUT /api/usuarios/me
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "nombre_granja": "Granja La Esperanza"
}
```

**Respuesta 200** (resumida)

```json
{
  "id_usuario": 1,
  "nombre_completo": "Ana Avicultora",
  "correo_electronico": "ana@example.com",
  "telefono": "3001234567",
  "nombre_granja": "Granja La Esperanza",
  "plan_suscripcion": "gratuito",
  "total_huevos_producidos": 4230,
  "total_aves_gestionadas": 118
}
```

### 5.3 Cambiar contraseña

**Solicitud**

```http
PUT /api/usuarios/me/contrasena
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "contrasena_actual": "Segura123!",
  "contrasena_nueva": "NuevaClave456!"
}
```

**Respuesta 200**

```json
{
  "mensaje": "Contraseña actualizada correctamente"
}
```

---

## 6. Errores comunes

| Situación | Código | Respuesta |
|---|---|---|
| Sin token | 401 | `{"detail": "No autenticado"}` |
| Token inválido o expirado | 401 | `{"detail": "Token inválido o expirado"}` |
| Recurso inexistente o de otro usuario | 404 | `{"detail": "Cliente no encontrado"}` |
| Correo o categoría duplicados | 409 | `{"detail": "El correo ya está registrado"}` |
| Regla de negocio incumplida | 400 | `{"detail": "Stock insuficiente para registrar la salida"}` |
| Validación automática (Pydantic) | 422 | `{"detail": [{"loc": ["body", "cantidad"], "msg": "Input should be greater than 0"}]}` |

Ejemplo de error de validación:

```http
POST /api/produccion
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "id_camada": 1,
  "fecha_recoleccion": "2026-01-01",
  "aa": 0,
  "a": 0,
  "b": 0,
  "no_apto": 0
}
```

**Respuesta 400**

```json
{
  "detail": "Solo se puede registrar producción del día actual"
}
```
