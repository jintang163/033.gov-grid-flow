from loguru import logger
from config import CONFIDENCE_THRESHOLD, DEPARTMENT_MAP
from model import BertClassifier
from rule_engine import RuleEngine
from schemas import ClassifyRequest, ClassifyResult


class DispatchService:
    def __init__(self):
        self.bert = BertClassifier()
        self.rule_engine = RuleEngine()
        self._model_loaded = False

    def load_model(self):
        self.bert.load_model()
        self._model_loaded = self.bert.loaded
        logger.info("DispatchService ready, BERT loaded: {}", self._model_loaded)

    def classify(self, request: ClassifyRequest) -> ClassifyResult:
        title = request.title
        description = request.description or ""
        event_type = request.event_type
        full_text = f"{title} {description}".strip()

        rule_result = self.rule_engine.classify(title, description, event_type)

        if self._model_loaded:
            model_result = self.bert.predict(full_text)

            if model_result["confidence"] > rule_result["confidence"]:
                dept_code = model_result["department_code"]
                confidence = model_result["confidence"]
                method = "model"
                all_scores = model_result.get("all_scores")
            elif rule_result["confidence"] > 0.5:
                dept_code = rule_result["department_code"]
                confidence = rule_result["confidence"]
                method = "rule"
                all_scores = None
            else:
                if model_result["confidence"] > 0.3:
                    dept_code = model_result["department_code"]
                    confidence = model_result["confidence"]
                    method = "model"
                    all_scores = model_result.get("all_scores")
                else:
                    dept_code = rule_result["department_code"]
                    confidence = rule_result["confidence"]
                    method = "rule"
                    all_scores = None
        else:
            dept_code = rule_result["department_code"]
            confidence = rule_result["confidence"]
            method = "rule"
            all_scores = None

        auto_dispatch = confidence >= CONFIDENCE_THRESHOLD

        return ClassifyResult(
            department_code=dept_code,
            department_name=DEPARTMENT_MAP.get(dept_code, "综合协调科"),
            confidence=round(confidence, 4),
            auto_dispatch=auto_dispatch,
            method=method,
            all_scores=all_scores
        )

    def train(self, records: list, epochs: int = 3, batch_size: int = 16, learning_rate: float = 2e-5) -> dict:
        if not self._model_loaded:
            raise RuntimeError("Model not loaded, cannot train")
        result = self.bert.train(records, epochs, batch_size, learning_rate)
        self._model_loaded = self.bert.loaded
        return result

    def is_model_loaded(self) -> bool:
        return self._model_loaded

    def get_model_info(self) -> dict:
        return {
            "bert_loaded": self._model_loaded,
            "device": str(self.bert.device),
            "label_count": len(self.bert.label_map),
            "confidence_threshold": CONFIDENCE_THRESHOLD
        }
