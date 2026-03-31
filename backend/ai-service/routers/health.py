from fastapi import APIRouter

router = APIRouter()


@router.get("/actuator/health")
async def health():
    return {"status": "UP"}
