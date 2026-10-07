package com.adso.eggchecker.data.remote

import com.adso.eggchecker.data.remote.dto.CamadaCreateDto
import com.adso.eggchecker.data.remote.dto.CamadaDto
import com.adso.eggchecker.data.remote.dto.CamadaUpdateDto
import com.adso.eggchecker.data.remote.dto.CategoriaCreateDto
import com.adso.eggchecker.data.remote.dto.CategoriaDto
import com.adso.eggchecker.data.remote.dto.CategoriaUpdateDto
import com.adso.eggchecker.data.remote.dto.CambiarEstadoDto
import com.adso.eggchecker.data.remote.dto.ClienteCreateDto
import com.adso.eggchecker.data.remote.dto.ClienteDto
import com.adso.eggchecker.data.remote.dto.ClienteEliminarDto
import com.adso.eggchecker.data.remote.dto.ClienteUpdateDto
import com.adso.eggchecker.data.remote.dto.InsumoCreateDto
import com.adso.eggchecker.data.remote.dto.InsumoDto
import com.adso.eggchecker.data.remote.dto.InsumoUpdateDto
import com.adso.eggchecker.data.remote.dto.LoginRequestDto
import com.adso.eggchecker.data.remote.dto.MensajeResponseDto
import com.adso.eggchecker.data.remote.dto.MortalidadRequestDto
import com.adso.eggchecker.data.remote.dto.MovimientoCreateDto
import com.adso.eggchecker.data.remote.dto.MovimientoDto
import com.adso.eggchecker.data.remote.dto.NotificacionDto
import com.adso.eggchecker.data.remote.dto.PedidoCreateDto
import com.adso.eggchecker.data.remote.dto.PedidoDto
import com.adso.eggchecker.data.remote.dto.PedidoEliminarDto
import com.adso.eggchecker.data.remote.dto.PedidoUpdateDto
import com.adso.eggchecker.data.remote.dto.ProduccionConDetalleDto
import com.adso.eggchecker.data.remote.dto.ProduccionCreateDto
import com.adso.eggchecker.data.remote.dto.ProduccionDto
import com.adso.eggchecker.data.remote.dto.RecuperarRequestDto
import com.adso.eggchecker.data.remote.dto.RegistroRequestDto
import com.adso.eggchecker.data.remote.dto.ReporteConsolidadoDto
import com.adso.eggchecker.data.remote.dto.StockDto
import com.adso.eggchecker.data.remote.dto.TokenResponseDto
import com.adso.eggchecker.data.remote.dto.UsuarioDto

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints del backend EggChecker consumidos por la app.
 * Las rutas son relativas a la base URL (termina en /api/).
 */
interface ApiService {

    /** Inicia sesión y devuelve el token de acceso. */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    /** Registra un avicultor nuevo. */
    @POST("auth/registro")
    suspend fun registrar(@Body body: RegistroRequestDto): UsuarioDto

    /** Consulta el perfil del usuario autenticado. */
    @GET("usuarios/me")
    suspend fun obtenerPerfil(): UsuarioDto

    /** Solicita la recuperación de contraseña. */
    @POST("auth/recuperar")
    suspend fun recuperar(@Body body: RecuperarRequestDto): MensajeResponseDto

    /** Lista las notificaciones del usuario (no leídas primero). */
    @GET("notificaciones")
    suspend fun listarNotificaciones(): List<NotificacionDto>

    /** Marca una notificación como leída. */
    @POST("notificaciones/{id}/leer")
    suspend fun marcarNotificacionLeida(
        @Path("id") idNotificacion: Int
    ): NotificacionDto

    /** Marca todas las notificaciones como leídas. */
    @POST("notificaciones/leer-todas")
    suspend fun marcarTodasLeidas()

    /** Elimina una notificación del usuario. */
    @DELETE("notificaciones/{id}")
    suspend fun eliminarNotificacion(@Path("id") idNotificacion: Int)

    /** Lista las camadas del usuario, opcionalmente filtradas por estado. */
    @GET("camadas")
    suspend fun listarCamadas(@Query("estado") estado: String?): List<CamadaDto>

    /** Obtiene una camada con su edad y retiro estimado. */
    @GET("camadas/{id}")
    suspend fun obtenerCamada(@Path("id") idCamada: Int): CamadaDto

    /** Registra una camada nueva. */
    @POST("camadas")
    suspend fun crearCamada(@Body body: CamadaCreateDto): CamadaDto

    /** Edita el nombre y, dentro de 24 h, la cantidad inicial. */
    @PATCH("camadas/{id}")
    suspend fun actualizarCamada(
        @Path("id") idCamada: Int,
        @Body body: CamadaUpdateDto
    ): CamadaDto

    /** Registra mortalidad y descuenta aves de la camada. */
    @POST("camadas/{id}/mortalidad")
    suspend fun registrarMortalidad(
        @Path("id") idCamada: Int,
        @Body body: MortalidadRequestDto
    ): CamadaDto

    /** Suma una semana de vida a la camada. */
    @POST("camadas/{id}/avanzar-semana")
    suspend fun avanzarSemana(@Path("id") idCamada: Int): CamadaDto

    /** Posponer una semana la decisión de la camada. */
    @POST("camadas/{id}/seguir-activa")
    suspend fun seguirActiva(@Path("id") idCamada: Int): CamadaDto

    /** Descarta una camada (estado retirada, irreversible). */
    @POST("camadas/{id}/descartar")
    suspend fun descartarCamada(@Path("id") idCamada: Int): CamadaDto

    /** Lista la producción del usuario, opcionalmente filtrada. */
    @GET("produccion")
    suspend fun listarProduccion(
        @Query("camada") camada: Int?,
        @Query("fecha") fecha: String?
    ): List<ProduccionDto>

    /** Obtiene una producción con su detalle por tipo. */
    @GET("produccion/{id}")
    suspend fun obtenerProduccion(
        @Path("id") idProduccion: Int
    ): ProduccionConDetalleDto

    /** Registra o actualiza la recolección diaria de una camada. */
    @POST("produccion")
    suspend fun registrarProduccion(
        @Body body: ProduccionCreateDto
    ): ProduccionConDetalleDto

    /** Lista el catálogo global de categorías de insumo. */
    @GET("categorias-insumo")
    suspend fun listarCategorias(): List<CategoriaDto>

    /** Crea una categoría de insumo. */
    @POST("categorias-insumo")
    suspend fun crearCategoria(@Body body: CategoriaCreateDto): CategoriaDto

    /** Edita una categoría de insumo. */
    @PATCH("categorias-insumo/{id}")
    suspend fun actualizarCategoria(
        @Path("id") idCategoria: Int,
        @Body body: CategoriaUpdateDto
    ): CategoriaDto

    /** Elimina una categoría sin insumos asociados. */
    @DELETE("categorias-insumo/{id}")
    suspend fun eliminarCategoria(@Path("id") idCategoria: Int)

    /** Lista los insumos del usuario, opcionalmente filtrados. */
    @GET("insumos")
    suspend fun listarInsumos(
        @Query("categoria") categoria: Int?,
        @Query("activo") activo: Boolean?
    ): List<InsumoDto>

    /** Registra un insumo nuevo. */
    @POST("insumos")
    suspend fun crearInsumo(@Body body: InsumoCreateDto): InsumoDto

    /** Edita nombre, unidad y umbral de un insumo. */
    @PATCH("insumos/{id}")
    suspend fun actualizarInsumo(
        @Path("id") idInsumo: Int,
        @Body body: InsumoUpdateDto
    ): InsumoDto

    /** Suspende un insumo (reversible). */
    @POST("insumos/{id}/suspender")
    suspend fun suspenderInsumo(@Path("id") idInsumo: Int): InsumoDto

    /** Reactiva un insumo suspendido. */
    @POST("insumos/{id}/activar")
    suspend fun activarInsumo(@Path("id") idInsumo: Int): InsumoDto

    /** Descontinúa un insumo (permanente). */
    @POST("insumos/{id}/descontinuar")
    suspend fun descontinuarInsumo(@Path("id") idInsumo: Int): InsumoDto

    /** Registra una entrada o salida de stock. */
    @POST("insumos/{id}/movimientos")
    suspend fun registrarMovimiento(
        @Path("id") idInsumo: Int,
        @Body body: MovimientoCreateDto
    ): MovimientoDto

    /** Lista los clientes del usuario incluyendo los suspendidos. */
    @GET("clientes")
    suspend fun listarClientes(@Query("activo") activo: Boolean?): List<ClienteDto>

    /** Registra un cliente nuevo. */
    @POST("clientes")
    suspend fun crearCliente(@Body body: ClienteCreateDto): ClienteDto

    /** Edita nombre, teléfono y dirección de un cliente. */
    @PATCH("clientes/{id}")
    suspend fun actualizarCliente(
        @Path("id") idCliente: Int,
        @Body body: ClienteUpdateDto
    ): ClienteDto

    /** Suspende un cliente (reversible). */
    @POST("clientes/{id}/suspender")
    suspend fun suspenderCliente(@Path("id") idCliente: Int): ClienteDto

    /** Reactiva un cliente suspendido. */
    @POST("clientes/{id}/activar")
    suspend fun activarCliente(@Path("id") idCliente: Int): ClienteDto

    /** Elimina un cliente de forma permanente confirmando la contraseña. */
    @POST("clientes/{id}/eliminar")
    suspend fun eliminarCliente(
        @Path("id") idCliente: Int,
        @Body body: ClienteEliminarDto
    )

    /** Stock de huevos disponible por tipo. */
    @GET("ventas/stock")
    suspend fun obtenerStock(): StockDto

    /** Lista los pedidos del usuario. */
    @GET("ventas/pedidos")
    suspend fun listarPedidos(): List<PedidoDto>

    /** Registra un pedido nuevo. */
    @POST("ventas/pedidos")
    suspend fun crearPedido(@Body body: PedidoCreateDto): PedidoDto

    /** Edita un pedido pendiente. */
    @PATCH("ventas/pedidos/{id}")
    suspend fun actualizarPedido(
        @Path("id") idPedido: Int,
        @Body body: PedidoUpdateDto
    ): PedidoDto

    /** Avanza el estado de un pedido a enviado o recibido. */
    @PATCH("ventas/pedidos/{id}/estado")
    suspend fun cambiarEstadoPedido(
        @Path("id") idPedido: Int,
        @Body body: CambiarEstadoDto
    ): PedidoDto

    /** Cancela un pedido y repone el stock. */
    @POST("ventas/pedidos/{id}/cancelar")
    suspend fun cancelarPedido(@Path("id") idPedido: Int): PedidoDto

    /** Elimina un pedido confirmando la contraseña. */
    @POST("ventas/pedidos/{id}/eliminar")
    suspend fun eliminarPedido(
        @Path("id") idPedido: Int,
        @Body body: PedidoEliminarDto
    )

    /** Reporte de rentabilidad consolidado del período. */
    @GET("reportes/consolidado")
    suspend fun obtenerReporteConsolidado(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?,
        @Query("camada") camada: Int?
    ): ReporteConsolidadoDto

    /** PDF del reporte de rentabilidad del período. */
    @GET("reportes/pdf")
    suspend fun descargarReportePdf(
        @Query("desde") desde: String?,
        @Query("hasta") hasta: String?,
        @Query("camada") camada: Int?
    ): ResponseBody
}
