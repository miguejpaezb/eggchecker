import { useLayoutEffect, useRef, useState } from 'react';

/**
 * Observa la altura real de un contenido y la expone para animarla.
 *
 * Se usa cuando el contenido de un contenedor cambia de tamaño (por ejemplo
 * al alternar entre dos formularios) y se quiere transicionar la altura sin
 * depender de valores fijos. El `ResizeObserver` también capta cambios
 * dinámicos, como la aparición de mensajes de error.
 *
 * @returns {{ contentRef: Object, height: number|null }} Referencia para el
 *   elemento de contenido y su altura medida en píxeles (null en el primer
 *   render).
 */
function useAnimatedHeight() {
  const contentRef = useRef(null);
  const [height, setHeight] = useState(null);

  useLayoutEffect(() => {
    const contenido = contentRef.current;
    if (!contenido) {
      return undefined;
    }

    const actualizar = () => setHeight(contenido.scrollHeight);
    actualizar();

    const observador = new ResizeObserver(actualizar);
    observador.observe(contenido);
    return () => observador.disconnect();
  }, []);

  return { contentRef, height };
}

export default useAnimatedHeight;
