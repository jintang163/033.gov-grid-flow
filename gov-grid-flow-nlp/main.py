import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from loguru import logger

from config import NLP_SERVICE_HOST, NLP_SERVICE_PORT, LOG_LEVEL
from schemas import (
    ClassifyRequest, ClassifyResponse, ClassifyResult,
    BatchClassifyRequest, BatchClassifyResponse,
    TrainRequest, TrainResponse, HealthResponse
)
from dispatch_service import DispatchService

logger.add("logs/nlp_service.log", rotation="10 MB", retention="7 days", level=LOG_LEVEL)

dispatch_service = DispatchService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Loading NLP model...")
    dispatch_service.load_model()
    logger.info("NLP service started on {}:{}", NLP_SERVICE_HOST, NLP_SERVICE_PORT)
    yield
    logger.info("NLP service shutting down")


app = FastAPI(
    title="政务网格事件智能分派 NLP 服务",
    description="基于BERT文本分类 + 规则引擎的事件智能分派服务",
    version="1.0.0",
    lifespan=lifespan
)


@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="ok",
        model_loaded=dispatch_service.is_model_loaded(),
        model_info=dispatch_service.get_model_info()
    )


@app.post("/classify", response_model=ClassifyResponse)
async def classify_event(request: ClassifyRequest):
    try:
        result = dispatch_service.classify(request)
        return ClassifyResponse(success=True, data=result, message="分类成功")
    except Exception as e:
        logger.error("Classify failed: {}", str(e))
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/classify/batch", response_model=BatchClassifyResponse)
async def classify_batch(request: BatchClassifyRequest):
    try:
        results = []
        for item in request.items:
            result = dispatch_service.classify(item)
            results.append(result)
        return BatchClassifyResponse(success=True, data=results, message=f"批量分类完成，共{len(results)}条")
    except Exception as e:
        logger.error("Batch classify failed: {}", str(e))
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/train", response_model=TrainResponse)
async def train_model(request: TrainRequest):
    try:
        if not dispatch_service.is_model_loaded():
            raise HTTPException(status_code=400, detail="模型未加载，无法训练")
        metrics = dispatch_service.train(
            request.records,
            request.epochs,
            request.batch_size,
            request.learning_rate
        )
        return TrainResponse(success=True, message="模型训练完成", metrics=metrics)
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Train failed: {}", str(e))
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=NLP_SERVICE_HOST,
        port=NLP_SERVICE_PORT,
        reload=False,
        log_level=LOG_LEVEL.lower()
    )
