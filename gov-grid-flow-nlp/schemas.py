from pydantic import BaseModel, Field
from typing import List, Optional


class ClassifyRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=200, description="事件标题")
    description: str = Field("", max_length=2000, description="事件描述")
    event_type: Optional[str] = Field(None, description="事件类型编码")
    event_id: Optional[str] = Field(None, description="事件ID，用于关联")


class ClassifyResult(BaseModel):
    department_code: str = Field(..., description="推荐部门编码")
    department_name: str = Field(..., description="推荐部门名称")
    confidence: float = Field(..., ge=0.0, le=1.0, description="分类置信度")
    auto_dispatch: bool = Field(..., description="是否自动分派（置信度>阈值）")
    method: str = Field(..., description="分类方法: rule/model/hybrid")
    all_scores: Optional[List[dict]] = Field(None, description="所有部门的置信度得分")


class ClassifyResponse(BaseModel):
    success: bool = True
    data: Optional[ClassifyResult] = None
    message: str = ""


class BatchClassifyRequest(BaseModel):
    items: List[ClassifyRequest] = Field(..., min_length=1, max_length=50)


class BatchClassifyResponse(BaseModel):
    success: bool = True
    data: List[ClassifyResult] = []
    message: str = ""


class TrainRequest(BaseModel):
    records: List[dict] = Field(..., min_length=10, description="训练数据")
    epochs: int = Field(3, ge=1, le=20, description="训练轮数")
    batch_size: int = Field(16, ge=4, le=64, description="批大小")
    learning_rate: float = Field(2e-5, ge=1e-6, le=1e-3, description="学习率")


class TrainResponse(BaseModel):
    success: bool = True
    message: str = ""
    metrics: Optional[dict] = None


class HealthResponse(BaseModel):
    status: str = "ok"
    model_loaded: bool = False
    model_info: Optional[dict] = None
