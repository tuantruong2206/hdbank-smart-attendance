"""Chatbot API endpoints with real DB-backed responses."""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from services.chatbot_service import ChatbotService

router = APIRouter()
chatbot = ChatbotService()


class ChatRequest(BaseModel):
    employee_id: str
    message: str
    session_id: str | None = None


class ChatResponse(BaseModel):
    message: str
    intent: str | None
    actions: list[str] = []
    session_id: str | None = None


class SessionOut(BaseModel):
    session_id: str
    started_at: str | None
    last_message_at: str | None
    is_escalated: bool


class MessageOut(BaseModel):
    sender: str
    message: str
    intent: str | None
    created_at: str | None


class EscalateRequest(BaseModel):
    reason: str | None = None


@router.post("/message", response_model=ChatResponse)
async def send_message(request: ChatRequest):
    """Process a chatbot message using real DB data. RBAC via employee_id."""
    result = chatbot.process_message(
        employee_id=request.employee_id,
        message=request.message,
        session_id=request.session_id,
    )
    return ChatResponse(
        message=result["message"],
        intent=result.get("intent"),
        actions=result.get("actions", []),
        session_id=result.get("session_id"),
    )


@router.get("/sessions/{employee_id}", response_model=list[SessionOut])
async def get_sessions(employee_id: str):
    """Get chatbot session history for an employee from the database."""
    sessions = chatbot.get_sessions(employee_id)
    return [SessionOut(**s) for s in sessions]


@router.get("/sessions/{session_id}/messages", response_model=list[MessageOut])
async def get_session_messages(session_id: str):
    """Get all messages in a chatbot session."""
    messages = chatbot.get_session_messages(session_id)
    return [MessageOut(**m) for m in messages]


@router.post("/sessions/{session_id}/escalate")
async def escalate_session(session_id: str, request: EscalateRequest):
    """Mark a chatbot session as escalated to HR."""
    success = chatbot.escalate_session(session_id, reason=request.reason)
    if not success:
        raise HTTPException(status_code=404, detail="Session not found")
    return {"status": "escalated", "session_id": session_id}
