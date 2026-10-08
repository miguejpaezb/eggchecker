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
        GEMINI_API_KEY: Llave de Google AI Studio para el análisis de
            huevos con IA. Si está vacía, el módulo funciona en modo
            demostración (diagnóstico de ejemplo, sin llamar a Google).
        GEMINI_MODEL: Modelo de visión principal (plan gratuito).
        GEMINI_MODEL_RESPALDO: Modelo que se usa si el principal no
            responde o agotó su cupo.
        IA_TIMEOUT_SEGUNDOS: Tiempo máximo de espera de la respuesta de IA.
        IA_UPLOAD_DIR: Carpeta donde se guardan las fotos analizadas.
        IA_MAX_MB: Tamaño máximo de la foto que se acepta.
        IA_LIMITE_DIARIO: Análisis por usuario y día, para no agotar el
            cupo gratuito de la API.
    """

    DATABASE_URL: str = "sqlite:///./eggchecker.db"
    JWT_SECRET_KEY: str = "cambiar-esta-llave-en-produccion"
    JWT_ALGORITHM: str = "HS256"
    # 129600 minutos = 90 días: la sesión móvil se mantiene hasta 3 meses.
    JWT_EXPIRE_MINUTES: int = 129600
    RECOVERY_TOKEN_EXPIRE_MINUTES: int = 30
    FRONTEND_URL: str = "http://localhost:5173"
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-3.5-flash-lite"
    GEMINI_MODEL_RESPALDO: str = "gemini-3.1-flash-lite"
    IA_TIMEOUT_SEGUNDOS: float = 45.0
    IA_UPLOAD_DIR: str = "uploads/analisis"
    IA_MAX_MB: int = 8
    IA_LIMITE_DIARIO: int = 20

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
