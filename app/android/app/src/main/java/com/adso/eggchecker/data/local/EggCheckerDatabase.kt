package com.adso.eggchecker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Base de datos local SQLite de EggChecker. */
@Database(entities = [UsuarioEntity::class], version = 1, exportSchema = false)
abstract class EggCheckerDatabase : RoomDatabase() {

    /** DAO del perfil de usuario. */
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var instancia: EggCheckerDatabase? = null

        /** Entrega la instancia única de la base de datos. */
        fun obtener(context: Context): EggCheckerDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    EggCheckerDatabase::class.java,
                    "eggchecker.db"
                ).build().also { instancia = it }
            }
    }
}
