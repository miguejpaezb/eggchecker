package com.adso.eggchecker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

/** Acceso a los datos del usuario almacenados en Room. */
@Dao
interface UsuarioDao {

    /** Observa el perfil guardado (la app usa un único usuario activo). */
    @Query("SELECT * FROM usuario LIMIT 1")
    fun observar(): Flow<UsuarioEntity?>

    /** Devuelve el perfil guardado o null si aún no se ha sincronizado. */
    @Query("SELECT * FROM usuario LIMIT 1")
    suspend fun obtener(): UsuarioEntity?

    /** Inserta o reemplaza el perfil del usuario. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    /** Borra el perfil guardado al cerrar sesión. */
    @Query("DELETE FROM usuario")
    suspend fun limpiar()
}
