-- ============================================================
--  EggChecker — Datos de Prueba (SEED) para SQLite
--  Origen: datos de prueba canónicos de EggChecker
--  Adaptación: se eliminan USE, backticks y SET FOREIGN_KEY_CHECKS
--  (sentencias que SQLite no acepta). Ejecutar SIEMPRE después de
--  eggchecker_ddl.sql.
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- CATÁLOGO: tipo_huevo (4 registros — catálogo fijo)
-- ─────────────────────────────────────────────────────────────
INSERT INTO tipo_huevo (nombre_tipo, descripcion) VALUES
  ('AA',      'Huevo de primera calidad: cáscara dura, tamaño grande, sin defectos visibles'),
  ('A',       'Huevo de buena calidad: pequeñas imperfecciones en cáscara o tamaño'),
  ('B',       'Huevo con defectos visibles: cáscara rugosa, tamaño pequeño o manchas'),
  ('No_apto', 'Huevo roto, con cáscara blanda, deformaciones graves o contaminado');


-- ─────────────────────────────────────────────────────────────
-- CATÁLOGO: categoria_insumo (6 registros — catálogo fijo)
-- ─────────────────────────────────────────────────────────────
INSERT INTO categoria_insumo (nombre_categ, descripcion) VALUES
  ('alimento',    'Concentrados, maíz, forraje y demás alimentos para las aves'),
  ('medicamento', 'Antibióticos, antiparasitarios y medicamentos veterinarios'),
  ('vitamina',    'Suplementos vitamínicos y minerales para las aves'),
  ('herramienta', 'Equipos y utensilios para el manejo del galpón'),
  ('limpieza',    'Desinfectantes, detergentes y materiales de aseo'),
  ('empaque',     'Cartones, canastas y embalajes para la distribución de huevos');


-- ─────────────────────────────────────────────────────────────
-- USUARIOS (10 registros) — id_usuario: 1 al 10
-- Plan premium: 1, 2, 4, 6, 8 / Gratuito: 3, 5, 7, 9, 10
-- Contraseña de prueba: Test1234! (bcrypt hash de ejemplo)
-- ─────────────────────────────────────────────────────────────
INSERT INTO usuario
  (nombre_completo, correo_electronico, contrasena_hash, telefono, plan_suscripcion, fecha_registro)
VALUES
  ('Brian Gonzalo Suárez Acevedo',  'brian.suarez@soy.sena.edu.co',   '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3001234567', 'premium',  '2025-07-01'),
  ('Lucía Fernanda Torres Ríos',    'lucia.torres@gmail.com',          '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3112345678', 'premium',  '2025-08-15'),
  ('Carlos Andrés Mejía Vargas',    'carlos.mejia@hotmail.com',        '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3209876543', 'gratuito', '2025-09-01'),
  ('María Isabel Cano Ospina',      'maria.cano@yahoo.com',            '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3154567890', 'premium',  '2025-10-10'),
  ('Jorge Ernesto Patiño Gómez',    'jorge.patino@gmail.com',          '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3187654321', 'gratuito', '2025-11-20'),
  ('Sandra Milena Rojas Herrera',   'sandra.rojas@outlook.com',        '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3001112233', 'premium',  '2026-01-05'),
  ('Andrés Felipe Mora Castillo',   'andres.mora@gmail.com',           '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3123334455', 'gratuito', '2026-01-18'),
  ('Diana Carolina Peña Salazar',   'diana.pena@gmail.com',            '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3045556677', 'premium',  '2026-02-01'),
  ('Héctor Fabio Zuluaga Arango',   'hector.zuluaga@hotmail.com',      '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3178889900', 'gratuito', '2026-02-14'),
  ('Paula Andrea Ríos Montoya',     'paula.rios@outlook.com',          '$2b$10$exampleHashForTestingPurposesOnlyXXXXXXXXX', '3209991122', 'gratuito', '2026-03-01');


-- ─────────────────────────────────────────────────────────────
-- CAMADAS (12 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO camada
  (id_usuario, nombre_camada, fecha_ingreso, cantidad_inicial, cantidad_actual, estado)
VALUES
  (1,  'Camada La Esperanza',    '2025-07-10', 50,  48, 'activa'),
  (2,  'Camada Villa Nueva',     '2025-08-20', 80,  77, 'activa'),
  (2,  'Camada San José',        '2025-12-01', 60,  58, 'activa'),
  (3,  'Lote El Paraíso',        '2025-09-05', 100, 95, 'activa'),
  (4,  'Camada Las Palmas',      '2025-10-15', 70,  70, 'activa'),
  (4,  'Camada Retirada 2025',   '2025-03-01', 50,  42, 'retirada'),
  (5,  'Lote Río Verde',         '2025-11-25', 120, 118, 'activa'),
  (6,  'Camada La Colina',       '2026-01-10', 90,  88, 'activa'),
  (7,  'Lote El Porvenir',       '2026-01-20', 45,  45, 'activa'),
  (8,  'Camada Santa Rosa',      '2026-02-01', 75,  73, 'activa'),
  (9,  'Lote Los Naranjos',      '2026-02-15', 55,  55, 'activa'),
  (10, 'Camada La Primavera',    '2026-03-01', 40,  40, 'activa');


-- ─────────────────────────────────────────────────────────────
-- INSUMOS (14 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO insumo
  (id_usuario, id_categoria, nombre_insumo, unidad_medida, stock_actual, umbral_minimo)
VALUES
  (1,  1, 'Concentrado Ponedora 16%',    'kg',    80.00, 20.00),
  (2,  1, 'Maíz molido',                 'kg',   150.00, 30.00),
  (2,  2, 'Oxitetraciclina 20%',         'ml',    50.00, 15.00),
  (3,  1, 'Concentrado Ponedora 18%',    'kg',   200.00, 40.00),
  (3,  5, 'Amonio cuaternario 5L',       'litro',  8.00, 10.00),  -- bajo umbral (alerta)
  (4,  1, 'Purina Ponedora',             'kg',    90.00, 25.00),
  (4,  3, 'Vitamina AD3E',               'ml',    30.00, 10.00),
  (5,  6, 'Cartones x30 unidades',       'unidad',200.00, 50.00),
  (5,  1, 'Sorgo forrajero',             'kg',   180.00, 35.00),
  (6,  4, 'Bebedero automático 5L',      'unidad',  6.00,  2.00),
  (6,  2, 'Ivermectina 1%',              'ml',    20.00, 10.00),
  (7,  1, 'Concentrado Inicio Ponedoras','kg',    40.00, 20.00),
  (8,  5, 'Desinfectante Virkon S',      'kg',     3.00, 10.00),  -- bajo umbral (alerta)
  (9,  2, 'Enrofloxacina 10%',           'ml',    25.00, 10.00);


-- ─────────────────────────────────────────────────────────────
-- MOVIMIENTOS DE INSUMO (14 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO movimiento_insumo
  (id_insumo, tipo_movimiento, cantidad, fecha_movimiento, observaciones)
VALUES
  (1,  'entrada', 100.00, '2025-07-11 08:00:00', 'Stock inicial concentrado ponedora'),
  (1,  'salida',   20.00, '2025-08-01 07:00:00', 'Consumo mensual camada La Esperanza'),
  (2,  'entrada', 180.00, '2025-08-21 08:00:00', 'Compra inicial de maíz molido'),
  (2,  'salida',   30.00, '2025-09-15 07:30:00', 'Consumo semanal camada Villa Nueva'),
  (3,  'entrada',  65.00, '2025-09-06 09:00:00', 'Compra de oxitetraciclina'),
  (4,  'entrada', 240.00, '2025-09-06 09:15:00', 'Primer pedido concentrado 18%'),
  (4,  'salida',   40.00, '2025-10-01 07:00:00', 'Consumo mensual Lote El Paraíso'),
  (5,  'entrada',  18.00, '2025-10-16 10:00:00', 'Compra amonio cuaternario'),
  (5,  'salida',   10.00, '2025-11-05 08:00:00', 'Desinfección galpón'),
  (6,  'entrada', 115.00, '2025-11-26 08:30:00', 'Compra Purina Ponedora'),
  (8,  'entrada', 200.00, '2025-11-26 09:00:00', 'Primer lote de cartones'),
  (11, 'entrada',  30.00, '2026-01-11 08:00:00', 'Compra ivermectina'),
  (13, 'entrada',  13.00, '2026-02-02 08:00:00', 'Stock inicial Virkon S'),
  (14, 'entrada',  25.00, '2026-02-16 09:00:00', 'Compra enrofloxacina');


-- ─────────────────────────────────────────────────────────────
-- EVENTOS SANITARIOS (10 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO evento_sanitario
  (id_camada, tipo_evento, fecha_evento, descripcion, mortalidad)
VALUES
  (1,  'vacunacion', '2025-07-20', 'Vacunación Newcastle dosis 1 — camada La Esperanza', 0),
  (2,  'vacunacion', '2025-09-01', 'Vacunación Newcastle dosis 1 — camada Villa Nueva', 0),
  (2,  'mortalidad', '2025-10-10', 'Tres aves muertas por calor excesivo en el galpón', 3),
  (3,  'vacunacion', '2025-09-10', 'Vacunación Marek — camada San José al ingreso', 0),
  (4,  'vacunacion', '2025-09-20', 'Vacunación Marek — Lote El Paraíso al ingreso', 0),
  (5,  'enfermedad', '2025-11-03', 'Brote leve de bronquitis infecciosa, tratamiento con oxitetraciclina', 0),
  (5,  'vacunacion', '2025-11-15', 'Vacunación Newcastle dosis 2 — refuerzo Camada Las Palmas', 0),
  (7,  'vacunacion', '2025-12-05', 'Vacunación Gumboro — Lote Río Verde', 0),
  (7,  'mortalidad', '2025-12-20', 'Dos aves muertas, causa indeterminada', 2),
  (8,  'vacunacion', '2026-01-20', 'Vacunación Newcastle dosis 1 — Camada La Colina', 0);


-- ─────────────────────────────────────────────────────────────
-- PRODUCCIÓN DIARIA (12 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO produccion_diaria
  (id_usuario, id_camada, fecha_recoleccion, total_huevos, observaciones)
VALUES
  (1,  1,  '2026-03-01',  42, 'Producción estable'),
  (1,  1,  '2026-03-02',  40, NULL),
  (2,  2,  '2026-03-01',  65, 'Producción normal'),
  (2,  2,  '2026-03-02',  63, NULL),
  (2,  3,  '2026-03-01',  50, 'Primera recolección camada San José'),
  (3,  4,  '2026-03-01',  88, 'Producción alta, clima favorable'),
  (4,  5,  '2026-03-01',  60, NULL),
  (5,  7,  '2026-03-01', 108, 'Récord del mes en Lote Río Verde'),
  (5,  7,  '2026-03-02', 105, NULL),
  (6,  8,  '2026-03-01',  78, NULL),
  (8,  10, '2026-03-01',  68, 'Producción normal camada Santa Rosa'),
  (10, 12, '2026-03-01',  32, 'Camada reciente, producción en ascenso');


-- ─────────────────────────────────────────────────────────────
-- PRODUCCIÓN DETALLE (48 registros)
-- id_tipo: 1=AA  2=A  3=B  4=No_apto
-- ─────────────────────────────────────────────────────────────
INSERT INTO produccion_detalle
  (id_produccion, id_tipo, cantidad)
VALUES
  (1,  1, 20), (1,  2, 15), (1,  3,  5), (1,  4,  2),
  (2,  1, 18), (2,  2, 15), (2,  3,  5), (2,  4,  2),
  (3,  1, 30), (3,  2, 25), (3,  3,  8), (3,  4,  2),
  (4,  1, 28), (4,  2, 25), (4,  3,  8), (4,  4,  2),
  (5,  1, 20), (5,  2, 20), (5,  3,  8), (5,  4,  2),
  (6,  1, 45), (6,  2, 30), (6,  3, 10), (6,  4,  3),
  (7,  1, 25), (7,  2, 22), (7,  3, 10), (7,  4,  3),
  (8,  1, 60), (8,  2, 35), (8,  3, 10), (8,  4,  3),
  (9,  1, 58), (9,  2, 34), (9,  3, 10), (9,  4,  3),
  (10, 1, 40), (10, 2, 25), (10, 3, 10), (10, 4,  3),
  (11, 1, 35), (11, 2, 22), (11, 3,  8), (11, 4,  3),
  (12, 1, 12), (12, 2, 13), (12, 3,  5), (12, 4,  2);


-- ─────────────────────────────────────────────────────────────
-- CLIENTES (12 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO cliente
  (id_usuario, nombre_cliente, telefono, direccion, latitud, longitud, fecha_ultima_compra)
VALUES
  (1,  'Tienda La Cosecha',          '3204440011', 'Cra 3 # 8-15, Bogotá',           4.7110,  -74.0721, '2026-02-28'),
  (1,  'Supermercado El Trigal',     '3158881122', 'Av 68 # 22-10, Bogotá',           4.6800,  -74.0900, '2026-03-01'),
  (2,  'Tienda Don Ramón',           '3204441122', 'Cra 5 # 12-34, Pereira',          4.8133,  -75.6961, '2026-02-28'),
  (2,  'Supermercado La 14',         '3154449988', 'Av Circunvalar # 45-10, Pereira', 4.8050,  -75.7200, '2026-03-01'),
  (3,  'Restaurante El Buen Sabor',  '3118887766', 'Cl 10 # 8-20, Armenia',           4.5340,  -75.6812, '2026-02-20'),
  (3,  'Minimercado Los Andes',      '3002223344', 'Cr 15 # 22-05, Armenia',          4.5280,  -75.6750, '2026-03-01'),
  (4,  'Fonda Campesina Yuly',       '3135556677', 'Vereda El Rosal, Manizales',      5.0680,  -75.5174, NULL),
  (5,  'Distribuidora HuevoFresh',   '3209990011', 'Cl 30 # 40-15, Medellín',         6.2442,  -75.5812, '2026-03-02'),
  (6,  'Asadero La Brasa Viva',      '3001234568', 'Av El Poblado # 55-30, Medellín', 6.2100,  -75.5650, '2026-02-15'),
  (7,  'Tienda La Hormiga',          '3117778899', 'Cra 8 # 5-60, Montería',          8.7575,  -75.8844, '2026-03-01'),
  (8,  'Minimercado Don Félix',      '3144445566', 'Cl 12 # 9-40, Bucaramanga',       7.1193,  -73.1227, NULL),
  (10, 'Tienda El Vecino',           '3056667788', 'Cra 6 # 3-20, Valledupar',       10.4631,  -73.2532, NULL);


-- ─────────────────────────────────────────────────────────────
-- PEDIDOS (12 registros)
-- ─────────────────────────────────────────────────────────────
INSERT INTO pedido
  (id_cliente, id_usuario, fecha_pedido, estado_pedido, valor_total)
VALUES
  (1,  1,  '2026-02-20', 'entregado',  62400.00),
  (2,  1,  '2026-03-01', 'pendiente',  48000.00),
  (3,  2,  '2026-02-25', 'entregado',  78000.00),
  (4,  2,  '2026-03-01', 'pendiente',  52000.00),
  (5,  3,  '2026-02-20', 'entregado',  96000.00),
  (6,  3,  '2026-03-01', 'pendiente',  48000.00),
  (7,  4,  '2026-02-28', 'cancelado',  36000.00),
  (8,  5,  '2026-03-01', 'entregado', 130000.00),
  (9,  6,  '2026-03-02', 'pendiente',  65000.00),
  (10, 7,  '2026-03-01', 'entregado',  84000.00),
  (11, 8,  '2026-03-01', 'pendiente',  54000.00),
  (12, 10, '2026-03-02', 'pendiente',  28000.00);


-- ─────────────────────────────────────────────────────────────
-- DETALLE DE PEDIDOS (20 registros)
-- id_tipo: 1=AA  2=A  3=B  4=No_apto
-- ─────────────────────────────────────────────────────────────
INSERT INTO detalle_pedido
  (id_pedido, id_tipo, cantidad, precio_unitario)
VALUES
  (1,  1,  96, 400.00), (1,  2,  60, 280.00),
  (2,  1,  80, 400.00), (2,  2,  40, 280.00),
  (3,  1, 120, 400.00), (3,  2,  75, 280.00),
  (4,  1,  80, 400.00), (4,  2,  60, 280.00),
  (5,  1, 150, 400.00), (5,  2,  80, 280.00), (5,  3, 30, 160.00),
  (6,  1,  60, 400.00), (6,  2,  80, 280.00),
  (7,  2, 100, 280.00), (7,  3,  50, 160.00),
  (8,  1, 200, 400.00), (8,  2, 100, 280.00), (8,  3, 50, 160.00),
  (9,  1, 100, 400.00), (9,  2,  75, 280.00);


-- ─────────────────────────────────────────────────────────────
-- ANÁLISIS IA (10 registros — solo usuarios premium: 1,2,4,6,8)
-- ─────────────────────────────────────────────────────────────
INSERT INTO analisis_ia
  (id_usuario, fecha_analisis, imagen_url, resultado_diagnostico, recomendaciones)
VALUES
  (1, '2026-03-01 09:00:00',
   'https://storage.eggchecker.app/u1/analisis_2026-03-01.jpg',
   'Muestra 30 huevos: 88% AA, 8% A, 4% No_apto. Sin anomalías de cáscara.',
   'Mantener condiciones actuales. Revisar aves con producción No_apto.'),
  (1, '2026-03-08 09:10:00',
   'https://storage.eggchecker.app/u1/analisis_2026-03-08.jpg',
   'Muestra 30 huevos: 90% AA, 7% A, 3% No_apto. Leve mejora respecto semana anterior.',
   'Continuar suplementación. Buena tendencia.'),
  (2, '2026-03-01 10:00:00',
   'https://storage.eggchecker.app/u2/analisis_2026-03-01.jpg',
   'Muestra 25 huevos: 80% AA, 16% A, 4% B. Leve rugosidad en algunas cáscaras.',
   'Suplementar calcio en la dieta. Revisar bebederos para asegurar hidratación.'),
  (2, '2026-03-05 10:15:00',
   'https://storage.eggchecker.app/u2/analisis_2026-03-05.jpg',
   'Muestra 25 huevos: 84% AA, 12% A, 4% B. Mejora en textura de cáscara.',
   'Mantener suplementación de calcio. Revisión semanal recomendada.'),
  (4, '2026-03-01 11:30:00',
   'https://storage.eggchecker.app/u4/analisis_2026-03-01.jpg',
   'Muestra 20 huevos: 72% A, 20% AA, 8% B. Tamaño homogéneo.',
   'Incrementar proteína en concentrado para mejorar proporción AA.'),
  (4, '2026-03-07 11:45:00',
   'https://storage.eggchecker.app/u4/analisis_2026-03-07.jpg',
   'Muestra 20 huevos: 75% AA, 18% A, 7% B. Leve mejora en clasificación.',
   'Continuar ajuste de dieta. Evaluar resultados en 15 días.'),
  (6, '2026-03-02 08:45:00',
   'https://storage.eggchecker.app/u6/analisis_2026-03-02.jpg',
   'Muestra 30 huevos: 85% AA, 12% A, 3% No_apto. Excelente uniformidad.',
   'Producción en buen estado. Considerar ampliar camada próximo ciclo.'),
  (6, '2026-03-09 08:50:00',
   'https://storage.eggchecker.app/u6/analisis_2026-03-09.jpg',
   'Muestra 30 huevos: 87% AA, 11% A, 2% No_apto. Tendencia positiva.',
   'Mantener manejo actual. Sistema en óptimas condiciones.'),
  (8, '2026-03-03 09:30:00',
   'https://storage.eggchecker.app/u8/analisis_2026-03-03.jpg',
   'Muestra 20 huevos: 78% AA, 18% A, 4% B. Buena calidad general.',
   'Revisar temperatura del galpón. Considerar ventilación adicional.'),
  (8, '2026-03-10 09:45:00',
   'https://storage.eggchecker.app/u8/analisis_2026-03-10.jpg',
   'Muestra 20 huevos: 82% AA, 15% A, 3% B. Mejora notable tras ajuste de ventilación.',
   'Continuar con las mejoras implementadas. Próximo análisis en 10 días.');

-- ============================================================
-- FIN DEL SEED SQLite — EggChecker v1.0
-- SENA ADSO 2026
-- ============================================================
