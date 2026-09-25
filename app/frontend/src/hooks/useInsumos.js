import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  actualizarCategoria,
  crearCategoria,
  eliminarCategoria,
  listarCategorias,
} from '../services/categoriaService';
import {
  activarInsumo,
  actualizarInsumo,
  anadirStock,
  crearInsumo,
  descontinuarInsumo,
  listarInsumos,
  suspenderInsumo,
} from '../services/insumoService';
import { aNumero, esCritico, esOptimo } from '../utils/inventario';

/**
 * Gestiona el inventario: listado, filtros, orden y acciones.
 * @returns {Object} Estado del inventario y acciones sobre insumos/categorías.
 */
function useInsumos() {
  const [insumos, setInsumos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [filtroStock, setFiltroStock] = useState('');
  const [filtroCategoria, setFiltroCategoria] = useState('');
  const [recarga, setRecarga] = useState(0);

  const refrescar = useCallback(() => setRecarga((valor) => valor + 1), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    Promise.all([listarInsumos(), listarCategorias()])
      .then(([datosInsumos, datosCategorias]) => {
        if (activo) {
          setInsumos(datosInsumos);
          setCategorias(datosCategorias);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setInsumos([]);
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [recarga]);

  const insumosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return insumos
      .filter(
        (insumo) => !texto || insumo.nombre_insumo.toLowerCase().includes(texto)
      )
      .filter(
        (insumo) =>
          !filtroCategoria || insumo.id_categoria === Number(filtroCategoria)
      )
      .filter((insumo) => {
        if (filtroStock === 'critico') {
          return esCritico(insumo);
        }
        if (filtroStock === 'optimo') {
          return esOptimo(insumo);
        }
        return true;
      })
      .sort((a, b) => aNumero(a.stock_actual) - aNumero(b.stock_actual));
  }, [insumos, busqueda, filtroCategoria, filtroStock]);

  const agregarInsumo = useCallback(
    async (datos) => {
      const creado = await crearInsumo(datos);
      refrescar();
      return creado;
    },
    [refrescar]
  );

  const editarInsumo = useCallback(
    async (idInsumo, datos) => {
      const actualizado = await actualizarInsumo(idInsumo, datos);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  const sumarStock = useCallback(
    async (idInsumo, cantidad) => {
      const movimiento = await anadirStock(idInsumo, cantidad);
      refrescar();
      return movimiento;
    },
    [refrescar]
  );

  const suspender = useCallback(
    async (idInsumo) => {
      const insumo = await suspenderInsumo(idInsumo);
      refrescar();
      return insumo;
    },
    [refrescar]
  );

  const activar = useCallback(
    async (idInsumo) => {
      const insumo = await activarInsumo(idInsumo);
      refrescar();
      return insumo;
    },
    [refrescar]
  );

  const descontinuar = useCallback(
    async (idInsumo) => {
      const insumo = await descontinuarInsumo(idInsumo);
      refrescar();
      return insumo;
    },
    [refrescar]
  );

  const agregarCategoria = useCallback(
    async (datos) => {
      const categoria = await crearCategoria(datos);
      refrescar();
      return categoria;
    },
    [refrescar]
  );

  const editarCategoria = useCallback(
    async (idCategoria, datos) => {
      const categoria = await actualizarCategoria(idCategoria, datos);
      refrescar();
      return categoria;
    },
    [refrescar]
  );

  const borrarCategoria = useCallback(
    async (idCategoria) => {
      await eliminarCategoria(idCategoria);
      refrescar();
    },
    [refrescar]
  );

  return {
    insumos,
    categorias,
    insumosFiltrados,
    cargando,
    error,
    busqueda,
    cambiarBusqueda: setBusqueda,
    filtroStock,
    cambiarFiltroStock: setFiltroStock,
    filtroCategoria,
    cambiarFiltroCategoria: setFiltroCategoria,
    agregarInsumo,
    editarInsumo,
    sumarStock,
    suspender,
    activar,
    descontinuar,
    agregarCategoria,
    editarCategoria,
    borrarCategoria,
  };
}

export default useInsumos;
