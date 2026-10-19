export const MODO_CADA_DIAS = 'cada_dias';
export const MODO_DIAS_SEMANA = 'dias_semana';
export const MODO_FECHAS = 'fechas';

export const MODOS = [
  { valor: MODO_CADA_DIAS, label: 'Cada N días' },
  { valor: MODO_DIAS_SEMANA, label: 'Días de la semana' },
  { valor: MODO_FECHAS, label: 'Fechas exactas' },
];

export const DIAS_SEMANA = [
  { valor: 0, label: 'Lun', largo: 'Lunes' },
  { valor: 1, label: 'Mar', largo: 'Martes' },
  { valor: 2, label: 'Mié', largo: 'Miércoles' },
  { valor: 3, label: 'Jue', largo: 'Jueves' },
  { valor: 4, label: 'Vie', largo: 'Viernes' },
  { valor: 5, label: 'Sáb', largo: 'Sábado' },
  { valor: 6, label: 'Dom', largo: 'Domingo' },
];

export const CATEGORIAS_SANITARIAS = [
  { valor: 'medicamento', label: 'Medicamento' },
  { valor: 'vitamina', label: 'Vitamina' },
];

export const MAXIMO_FECHAS = 4;

/** Fecha local en ISO (YYYY-MM-DD) sin desfase de zona horaria. */
function isoLocal(fecha) {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

/** Fecha de hoy en ISO local (YYYY-MM-DD). */
export function hoyIso() {
  return isoLocal(new Date());
}

/** Suma (o resta) días a una fecha ISO. */
export function sumarDias(fechaISO, dias) {
  const [anio, mes, dia] = fechaISO.split('-').map(Number);
  return isoLocal(new Date(anio, mes - 1, dia + dias));
}

/** Fecha ISO (YYYY-MM-DD) a DD/MM/AAAA. */
export function formatearFecha(fechaISO) {
  if (!fechaISO) {
    return '—';
  }
  const partes = fechaISO.split('-');
  if (partes.length !== 3) {
    return fechaISO;
  }
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

/** Formatea una cantidad sin decimales innecesarios (5.00 → "5"). */
export function formatoCantidad(valor) {
  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return '0';
  }
  return Number.isInteger(numero)
    ? String(numero)
    : numero.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

/** Cantidad con su unidad de medida ("5 ml"). */
export function textoCantidad(cantidad, unidad) {
  const numero = formatoCantidad(cantidad);
  return unidad ? `${numero} ${unidad}` : numero;
}

/** Resume cómo se repite un recordatorio para las listas. */
export function resumenModo(recordatorio) {
  if (recordatorio.modo === MODO_CADA_DIAS) {
    return recordatorio.intervalo_dias === 1
      ? 'Cada día'
      : `Cada ${recordatorio.intervalo_dias || 0} días`;
  }
  if (recordatorio.modo === MODO_DIAS_SEMANA) {
    return (recordatorio.dias_semana || [])
      .map((dia) => DIAS_SEMANA[dia]?.label)
      .filter(Boolean)
      .join(', ');
  }
  if (recordatorio.modo === MODO_FECHAS) {
    const total = (recordatorio.fechas || []).length;
    return total === 1 ? '1 fecha' : `${total} fechas`;
  }
  return recordatorio.modo;
}

/** Alcance del recordatorio: la camada o toda la granja. */
export function textoAlcance(recordatorio) {
  return recordatorio.id_camada
    ? recordatorio.nombre_camada || 'Camada'
    : 'Toda la granja';
}
