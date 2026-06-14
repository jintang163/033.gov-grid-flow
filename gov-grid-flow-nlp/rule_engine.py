import jieba
from config import KEYWORD_RULES, EVENT_TYPE_DEPT_RULES, DEPARTMENT_MAP
from loguru import logger


class RuleEngine:
    def __init__(self):
        self._init_jieba()
        self.keyword_rules = KEYWORD_RULES
        self.event_type_rules = EVENT_TYPE_DEPT_RULES
        self.dept_map = DEPARTMENT_MAP

    def _init_jieba(self):
        for keywords, _ in self.keyword_rules:
            for kw in keywords:
                jieba.add_word(kw)
        logger.info("RuleEngine initialized with {} keyword rules", len(self.keyword_rules))

    def classify_by_keywords(self, text: str) -> dict:
        if not text or not text.strip():
            return {"department_code": "other", "confidence": 0.0, "matched_keywords": []}

        matched_results = []
        for keywords, dept_code in self.keyword_rules:
            matched_kws = [kw for kw in keywords if kw in text]
            if matched_kws:
                matched_results.append({
                    "department_code": dept_code,
                    "matched_count": len(matched_kws),
                    "matched_keywords": matched_kws,
                    "confidence": min(0.6 + 0.1 * len(matched_kws), 0.95)
                })

        if not matched_results:
            return {"department_code": "other", "confidence": 0.0, "matched_keywords": []}

        matched_results.sort(key=lambda x: (x["matched_count"], x["confidence"]), reverse=True)
        best = matched_results[0]
        return best

    def classify_by_event_type(self, event_type: str) -> dict:
        if not event_type:
            return {"department_code": "other", "confidence": 0.3}

        dept_code = self.event_type_rules.get(event_type, "other")
        return {
            "department_code": dept_code,
            "confidence": 0.5 if dept_code != "other" else 0.2
        }

    def classify(self, title: str, description: str = "", event_type: str = None) -> dict:
        full_text = f"{title} {description}".strip()

        keyword_result = self.classify_by_keywords(full_text)

        event_type_result = self.classify_by_event_type(event_type) if event_type else None

        if keyword_result["confidence"] >= 0.6:
            return {
                "department_code": keyword_result["department_code"],
                "department_name": self.dept_map.get(keyword_result["department_code"], "综合协调科"),
                "confidence": keyword_result["confidence"],
                "method": "rule",
                "matched_keywords": keyword_result.get("matched_keywords", [])
            }

        if event_type_result and event_type_result["confidence"] > 0.3:
            return {
                "department_code": event_type_result["department_code"],
                "department_name": self.dept_map.get(event_type_result["department_code"], "综合协调科"),
                "confidence": event_type_result["confidence"],
                "method": "rule",
                "matched_keywords": keyword_result.get("matched_keywords", [])
            }

        return {
            "department_code": keyword_result.get("department_code", "other"),
            "department_name": self.dept_map.get(
                keyword_result.get("department_code", "other"), "综合协调科"
            ),
            "confidence": keyword_result.get("confidence", 0.0),
            "method": "rule",
            "matched_keywords": keyword_result.get("matched_keywords", [])
        }
