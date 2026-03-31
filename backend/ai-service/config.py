from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql://sa_admin:localdev123@localhost:5432/smart_attendance"
    REDIS_URL: str = "redis://:localdev123@localhost:6379/0"
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:29092"
    JWT_SECRET: str = "myLocalDevSecretKeyThatIsAtLeast256BitsLong1234567890"
    SERVICE_PORT: int = 8086

    class Config:
        env_file = ".env"


settings = Settings()
