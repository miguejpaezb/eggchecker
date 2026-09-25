-- ============================================================
--  EggChecker — Modelo Físico para SQLite (desarrollo / QA)
--  Origen: modelo relacional canónico de EggChecker
--  Adaptación del dialecto MySQL/MariaDB a SQLite:
--    * AUTO_INCREMENT  -> INTEGER PRIMARY KEY AUTOINCREMENT
--    * ENUM            -> TEXT + CHECK (columna IN (...))
--    * ENGINE/COLLATE  -> eliminados (no aplican en SQLite)
--    * Fechas y DATETIME -> TEXT (SQLite no tiene tipos nativos)
--    * PROCEDURES sp_* -> no aplican en SQLite; la lógica de negocio
--                          se implementa en app/backend/services/
--    * TRIGGER trg_*   -> no se portan en S0 (se decide en S1+)
--  Regla: los cambios al esquema canónico se hacen PRIMERO en
--  supporting_documentation/context/ y luego se reflejan aquí.
-- ============================================================

PRAGMA foreign_keys = ON;

-- ─────────────────────────────────────────────────────────────
-- Table usuario
-- Entidad central: avicultor registrado en el sistema.
-- Relaciones: 1:N con camada, insumo, produccion_diaria,
--             cliente, pedido, analisis_ia
-- ─────────────────────────────────────────────────────────────

CREATE TABLE usuario (
  id_usuario         INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
  nombre_completo    TEXT         NOT NULL,
  correo_electronico TEXT         NOT NULL,
  contrasena_hash    TEXT         NOT NULL,
  telefono           TEXT         NULL,
  plan_suscripcion   TEXT         NOT NULL DEFAULT 'gratuito',
  fecha_registro     TEXT         NOT NULL,
  activo             INTEGER      NOT NULL DEFAULT 1,
  CONSTRAINT uq_usuario_correo UNIQUE (correo_electronico),
  CONSTRAINT chk_usuario_plan
    CHECK (plan_suscripcion IN ('gratuito','premium'))
);


-- ─────────────────────────────────────────────────────────────
-- Table token_recuperacion
-- Tokens de recuperación de contraseña (uno por solicitud).
-- El token crudo nunca se guarda: solo su hash SHA-256.
-- FK: id_usuario → usuario
-- ─────────────────────────────────────────────────────────────

CREATE TABLE token_recuperacion (
  id_token         INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario       INTEGER NOT NULL,
  token_hash       TEXT    NOT NULL,
  fecha_creacion   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_expiracion TEXT    NOT NULL,
  usado            INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT fk_token_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_token_usuario ON token_recuperacion (id_usuario);
CREATE INDEX idx_token_hash    ON token_recuperacion (token_hash);


-- ─────────────────────────────────────────────────────────────
-- Table categoria_insumo
-- Catálogo normalizado de tipos de insumo (3FN).
-- ─────────────────────────────────────────────────────────────

CREATE TABLE categoria_insumo (
  id_categoria INTEGER      NOT NULL PRIMARY KEY AUTOINCREMENT,
  nombre_categ TEXT         NOT NULL,
  descripcion  TEXT         NULL,
  CONSTRAINT uq_categoria_nombre UNIQUE (nombre_categ)
);


-- ─────────────────────────────────────────────────────────────
-- Table insumo
-- Inventario de recursos del avicultor.
-- FK: id_usuario → usuario, id_categoria → categoria_insumo
-- ─────────────────────────────────────────────────────────────

CREATE TABLE insumo (
  id_insumo     INTEGER        NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario    INTEGER        NOT NULL,
  id_categoria  INTEGER        NOT NULL,
  nombre_insumo TEXT           NOT NULL,
  unidad_medida TEXT           NOT NULL,
  stock_actual  DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
  umbral_minimo DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
  activo        INTEGER        NOT NULL DEFAULT 1,
  descontinuado INTEGER        NOT NULL DEFAULT 0,
  CONSTRAINT fk_insumo_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_insumo_categoria
    FOREIGN KEY (id_categoria)
    REFERENCES categoria_insumo (id_categoria)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_insumo_stock
    CHECK (stock_actual  >= 0),
  CONSTRAINT chk_insumo_umbral
    CHECK (umbral_minimo >= 0),
  CONSTRAINT chk_insumo_descontinuado
    CHECK (descontinuado IN (0,1))
);

CREATE INDEX idx_insumo_usuario   ON insumo (id_usuario);
CREATE INDEX idx_insumo_categoria ON insumo (id_categoria);


-- ─────────────────────────────────────────────────────────────
-- Table movimiento_insumo
-- Trazabilidad de cada entrada y salida de un insumo.
-- FK: id_insumo → insumo
-- ─────────────────────────────────────────────────────────────

CREATE TABLE movimiento_insumo (
  id_movimiento    INTEGER       NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_insumo        INTEGER       NOT NULL,
  tipo_movimiento  TEXT          NOT NULL,
  cantidad         DECIMAL(10,2) NOT NULL,
  fecha_movimiento TEXT          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  observaciones    TEXT          NULL,
  CONSTRAINT fk_movimiento_insumo
    FOREIGN KEY (id_insumo)
    REFERENCES insumo (id_insumo)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_movimiento_tipo
    CHECK (tipo_movimiento IN ('entrada','salida')),
  CONSTRAINT chk_movimiento_cantidad
    CHECK (cantidad > 0)
);

CREATE INDEX idx_movimiento_insumo ON movimiento_insumo (id_insumo);
CREATE INDEX idx_movimiento_fecha  ON movimiento_insumo (fecha_movimiento DESC);


-- ─────────────────────────────────────────────────────────────
-- Table camada
-- Lote de aves ponedoras de un avicultor.
-- FK: id_usuario → usuario
-- ─────────────────────────────────────────────────────────────

CREATE TABLE camada (
  id_camada        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario       INTEGER NOT NULL,
  nombre_camada    TEXT    NOT NULL,
  fecha_ingreso    TEXT    NOT NULL,
  cantidad_inicial INTEGER NOT NULL,
  cantidad_actual  INTEGER NOT NULL,
  estado           TEXT    NOT NULL DEFAULT 'activa',
  edad_semanas     INTEGER NOT NULL DEFAULT 16,
  fecha_proximo_aviso TEXT NULL,
  fecha_creacion   TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_camada_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_camada_estado
    CHECK (estado IN ('activa','retirada')),
  CONSTRAINT chk_camada_cant_ini
    CHECK (cantidad_inicial > 0),
  CONSTRAINT chk_camada_cant_act
    CHECK (cantidad_actual >= 0),
  CONSTRAINT chk_camada_cant_lte
    CHECK (cantidad_actual <= cantidad_inicial),
  CONSTRAINT chk_camada_edad
    CHECK (edad_semanas >= 16)
);

CREATE INDEX idx_camada_usuario ON camada (id_usuario);


-- ─────────────────────────────────────────────────────────────
-- Table evento_sanitario
-- Historial de vacunaciones, enfermedades y mortalidad.
-- FK: id_camada → camada
-- ─────────────────────────────────────────────────────────────

CREATE TABLE evento_sanitario (
  id_evento    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_camada    INTEGER NOT NULL,
  tipo_evento  TEXT    NOT NULL,
  fecha_evento TEXT    NOT NULL,
  descripcion  TEXT    NOT NULL,
  mortalidad   INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT fk_evento_camada
    FOREIGN KEY (id_camada)
    REFERENCES camada (id_camada)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_evento_tipo
    CHECK (tipo_evento IN ('vacunacion','mortalidad','enfermedad','otro')),
  CONSTRAINT chk_evento_mortalidad
    CHECK (mortalidad >= 0)
);

CREATE INDEX idx_evento_camada ON evento_sanitario (id_camada);
CREATE INDEX idx_evento_fecha  ON evento_sanitario (fecha_evento DESC);


-- ─────────────────────────────────────────────────────────────
-- Table tipo_huevo
-- Catálogo de clasificaciones: AA, A, B, No_apto.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE tipo_huevo (
  id_tipo     INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  nombre_tipo TEXT    NOT NULL,
  descripcion TEXT    NULL,
  CONSTRAINT uq_tipo_huevo_nombre UNIQUE (nombre_tipo)
);


-- ─────────────────────────────────────────────────────────────
-- Table produccion_diaria
-- Registro de recolección de huevos por fecha y camada.
-- FK: id_usuario → usuario, id_camada → camada
-- ─────────────────────────────────────────────────────────────

CREATE TABLE produccion_diaria (
  id_produccion     INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario        INTEGER NOT NULL,
  id_camada         INTEGER NOT NULL,
  fecha_recoleccion TEXT    NOT NULL,
  total_huevos      INTEGER NOT NULL DEFAULT 0,
  observaciones     TEXT    NULL,
  CONSTRAINT fk_prod_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_prod_camada
    FOREIGN KEY (id_camada)
    REFERENCES camada (id_camada)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_prod_usuario_camada_fecha
    UNIQUE (id_usuario, id_camada, fecha_recoleccion),
  CONSTRAINT chk_prod_total
    CHECK (total_huevos >= 0)
);

CREATE INDEX idx_prod_usuario ON produccion_diaria (id_usuario);
CREATE INDEX idx_prod_camada  ON produccion_diaria (id_camada);
CREATE INDEX idx_prod_fecha   ON produccion_diaria (fecha_recoleccion DESC);


-- ─────────────────────────────────────────────────────────────
-- Table produccion_detalle
-- Desglose por tipo de huevo (resuelve relación M:N).
-- FK: id_produccion → produccion_diaria, id_tipo → tipo_huevo
-- ─────────────────────────────────────────────────────────────

CREATE TABLE produccion_detalle (
  id_detalle    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_produccion INTEGER NOT NULL,
  id_tipo       INTEGER NOT NULL,
  cantidad      INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT fk_prodet_produccion
    FOREIGN KEY (id_produccion)
    REFERENCES produccion_diaria (id_produccion)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_prodet_tipo
    FOREIGN KEY (id_tipo)
    REFERENCES tipo_huevo (id_tipo)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_prodet_prod_tipo
    UNIQUE (id_produccion, id_tipo),
  CONSTRAINT chk_prodet_cantidad
    CHECK (cantidad >= 0)
);

CREATE INDEX idx_prodet_produccion ON produccion_detalle (id_produccion);
CREATE INDEX idx_prodet_tipo       ON produccion_detalle (id_tipo);


-- ─────────────────────────────────────────────────────────────
-- Table cliente
-- Compradores del avicultor.
-- FK: id_usuario → usuario
-- ─────────────────────────────────────────────────────────────

CREATE TABLE cliente (
  id_cliente          INTEGER       NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario          INTEGER       NOT NULL,
  nombre_cliente      TEXT          NOT NULL,
  telefono            TEXT          NULL,
  direccion           TEXT          NULL,
  latitud             DECIMAL(10,7) NULL,
  longitud            DECIMAL(10,7) NULL,
  fecha_ultima_compra TEXT          NULL,
  activo              INTEGER       NOT NULL DEFAULT 1,
  CONSTRAINT fk_cliente_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_cliente_usuario       ON cliente (id_usuario);
CREATE INDEX idx_cliente_ultima_compra ON cliente (fecha_ultima_compra DESC);


-- ─────────────────────────────────────────────────────────────
-- Table pedido
-- Solicitud de compra de un cliente.
-- FK: id_cliente → cliente, id_usuario → usuario
-- ─────────────────────────────────────────────────────────────

CREATE TABLE pedido (
  id_pedido     INTEGER       NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_cliente    INTEGER       NOT NULL,
  id_usuario    INTEGER       NOT NULL,
  fecha_pedido  TEXT          NOT NULL,
  estado_pedido TEXT          NOT NULL DEFAULT 'pendiente',
  valor_total   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_pedido_cliente
    FOREIGN KEY (id_cliente)
    REFERENCES cliente (id_cliente)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_pedido_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_pedido_estado
    CHECK (estado_pedido IN ('pendiente','entregado','cancelado')),
  CONSTRAINT chk_pedido_valor
    CHECK (valor_total >= 0)
);

CREATE INDEX idx_pedido_cliente ON pedido (id_cliente);
CREATE INDEX idx_pedido_usuario ON pedido (id_usuario);
CREATE INDEX idx_pedido_fecha   ON pedido (fecha_pedido DESC);
CREATE INDEX idx_pedido_estado  ON pedido (estado_pedido);


-- ─────────────────────────────────────────────────────────────
-- Table detalle_pedido
-- Líneas del pedido: tipo de huevo, cantidad y precio.
-- FK: id_pedido → pedido (CASCADE), id_tipo → tipo_huevo
-- ─────────────────────────────────────────────────────────────

CREATE TABLE detalle_pedido (
  id_detalle_pedido INTEGER       NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_pedido         INTEGER       NOT NULL,
  id_tipo           INTEGER       NOT NULL,
  cantidad          INTEGER       NOT NULL,
  precio_unitario   DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detped_pedido
    FOREIGN KEY (id_pedido)
    REFERENCES pedido (id_pedido)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_detped_tipo
    FOREIGN KEY (id_tipo)
    REFERENCES tipo_huevo (id_tipo)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_detped_pedido_tipo
    UNIQUE (id_pedido, id_tipo),
  CONSTRAINT chk_detped_cantidad
    CHECK (cantidad        > 0),
  CONSTRAINT chk_detped_precio
    CHECK (precio_unitario > 0)
);

CREATE INDEX idx_detped_pedido ON detalle_pedido (id_pedido);
CREATE INDEX idx_detped_tipo   ON detalle_pedido (id_tipo);


-- ─────────────────────────────────────────────────────────────
-- Table analisis_ia
-- Resultados del módulo IA de análisis de huevos (premium).
-- FK: id_usuario → usuario
-- ─────────────────────────────────────────────────────────────

CREATE TABLE analisis_ia (
  id_analisis           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario            INTEGER NOT NULL,
  fecha_analisis        TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  imagen_url            TEXT    NOT NULL,
  resultado_diagnostico TEXT    NOT NULL,
  recomendaciones       TEXT    NULL,
  CONSTRAINT fk_analisis_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_analisis_usuario ON analisis_ia (id_usuario);
CREATE INDEX idx_analisis_fecha   ON analisis_ia (fecha_analisis DESC);


-- ─────────────────────────────────────────────────────────────
-- Table notificacion
-- Avisos al avicultor (ej. camada que supera las 72 semanas).
-- FK: id_usuario → usuario, id_camada → camada
-- ─────────────────────────────────────────────────────────────

CREATE TABLE notificacion (
  id_notificacion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  id_usuario      INTEGER NOT NULL,
  id_camada       INTEGER NULL,
  id_insumo       INTEGER NULL,
  tipo            TEXT    NOT NULL,
  titulo          TEXT    NOT NULL,
  mensaje         TEXT    NOT NULL,
  leida           INTEGER NOT NULL DEFAULT 0,
  eliminada       INTEGER NOT NULL DEFAULT 0,
  stock_referencia DECIMAL(10,2) NULL,
  fecha_creacion  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notificacion_usuario
    FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_notificacion_camada
    FOREIGN KEY (id_camada)
    REFERENCES camada (id_camada)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_notificacion_insumo
    FOREIGN KEY (id_insumo)
    REFERENCES insumo (id_insumo)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_notificacion_leida
    CHECK (leida IN (0,1)),
  CONSTRAINT chk_notificacion_eliminada
    CHECK (eliminada IN (0,1)),
  CONSTRAINT uq_notificacion_usuario_camada_tipo
    UNIQUE (id_usuario, id_camada, tipo)
);

CREATE INDEX idx_notificacion_usuario ON notificacion (id_usuario, leida);


-- ─────────────────────────────────────────────────────────────
-- VISTAS
-- ─────────────────────────────────────────────────────────────

-- Vista: producción diaria con detalle por tipo de huevo
CREATE VIEW v_produccion_detallada AS
SELECT
    pd.id_produccion,
    u.nombre_completo          AS avicultor,
    c.nombre_camada,
    pd.fecha_recoleccion,
    pd.total_huevos,
    th.nombre_tipo             AS tipo_huevo,
    pdd.cantidad               AS cantidad_tipo
FROM  produccion_diaria  pd
JOIN  usuario            u   ON pd.id_usuario    = u.id_usuario
JOIN  camada             c   ON pd.id_camada     = c.id_camada
JOIN  produccion_detalle pdd ON pd.id_produccion = pdd.id_produccion
JOIN  tipo_huevo         th  ON pdd.id_tipo      = th.id_tipo
ORDER BY pd.fecha_recoleccion DESC, th.nombre_tipo ASC;

-- Vista: insumos bajo el umbral mínimo (alertas)
CREATE VIEW v_alertas_insumo AS
SELECT
    i.id_insumo,
    u.nombre_completo  AS avicultor,
    ci.nombre_categ    AS categoria,
    i.nombre_insumo,
    i.stock_actual,
    i.umbral_minimo,
    (i.umbral_minimo - i.stock_actual) AS deficit
FROM  insumo           i
JOIN  usuario          u  ON i.id_usuario   = u.id_usuario
JOIN  categoria_insumo ci ON i.id_categoria = ci.id_categoria
WHERE i.stock_actual < i.umbral_minimo
  AND i.activo = 1
ORDER BY deficit DESC;

-- Vista: pedidos pendientes con valor total
CREATE VIEW v_pedidos_pendientes AS
SELECT
    p.id_pedido,
    u.nombre_completo  AS avicultor,
    cl.nombre_cliente  AS cliente,
    cl.telefono        AS telefono_cliente,
    p.fecha_pedido,
    p.valor_total,
    COUNT(dp.id_detalle_pedido) AS num_items
FROM  pedido         p
JOIN  usuario        u  ON p.id_usuario = u.id_usuario
JOIN  cliente        cl ON p.id_cliente = cl.id_cliente
JOIN  detalle_pedido dp ON p.id_pedido  = dp.id_pedido
WHERE p.estado_pedido = 'pendiente'
GROUP BY p.id_pedido, u.nombre_completo, cl.nombre_cliente,
         cl.telefono, p.fecha_pedido, p.valor_total
ORDER BY p.fecha_pedido ASC;

-- ============================================================
-- FIN DEL DDL SQLite — eggchecker_db (desarrollo / QA)
-- EggChecker v1.0 — SENA ADSO 2026
-- ============================================================
