from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuración central de la aplicación.

    Los valores se leen desde variables de entorno o del archivo .env del
    directorio app/backend. Los secretos reales nunca se versionan.

    Attributes:
        DATABASE_URL: Cadena de conexión a la base de datos.
        JWT_SECRET_KEY: Llave secreta para firmar los tokens JWT.
        JWT_ALGORITHM: Algoritmo de firma de los tokens.
        JWT_EXPIRE_MINUTES: Minutos de validez de los tokens.
        RECOVERY_TOKEN_EXPIRE_MINUTES: Minutos de validez del token de
            recuperación de contraseña.
        FRONTEND_URL: URL base del frontend para armar el enlace de
            restablecimiento.
    """

    DATABASE_URL: str = "sqlite:///./eggchecker.db"
    JWT_SECRET_KEY: str = "cambiar-esta-llave-en-produccion"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 60
    RECOVERY_TOKEN_EXPIRE_MINUTES: int = 30
    FRONTEND_URL: str = "http://localhost:5173"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
    )


@lru_cache
def get_settings() -> Settings:
    """Entrega la configuración cacheada para toda la aplicación.

    Returns:
        Settings: Instancia única de configuración.
    """
    return Settings()
