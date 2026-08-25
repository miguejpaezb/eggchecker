from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Base declarativa de SQLAlchemy para todos los modelos ORM.

    Los modelos de negocio (camadas, producción, ventas) heredan de esta
    clase para que Alembic pueda autogenerar las migraciones.
    """
