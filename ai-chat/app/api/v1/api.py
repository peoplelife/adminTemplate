

from fastapi import APIRouter
from app.api.v1.rag import router as rag_router
from app.api.v1.rag_retrieval import router as rag_retrieval_router
from app.api.v1.rag_conversations import router as rag_conversations_router
from app.api.v1.health import router as health_router
from app.api.v1.rag_evaluation import router as rag_evaluation_router
from app.core.constants import ApiTag


api_router=APIRouter()

api_router.include_router(health_router, tags=[ApiTag.HEALTH])
api_router.include_router(rag_router,tags=[ApiTag.RAG])
api_router.include_router(rag_retrieval_router, tags=[ApiTag.RAG])
api_router.include_router(rag_conversations_router, tags=[ApiTag.RAG])
api_router.include_router(rag_evaluation_router, tags=[ApiTag.RAG])
