import os
import json
import torch
import numpy as np
from loguru import logger
from config import MODEL_NAME, MODEL_PATH, DEPARTMENT_MAP


class BertClassifier:
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.label_map = {i: code for i, code in enumerate(DEPARTMENT_MAP.keys())}
        self.id2label = {i: code for i, code in enumerate(DEPARTMENT_MAP.keys())}
        self.label2id = {code: i for i, code in enumerate(DEPARTMENT_MAP.keys())}
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.loaded = False

    def load_model(self):
        try:
            from transformers import BertTokenizer, BertForSequenceClassification

            if os.path.exists(MODEL_PATH) and os.path.exists(os.path.join(MODEL_PATH, "config.json")):
                logger.info("Loading fine-tuned model from {}", MODEL_PATH)
                self.tokenizer = BertTokenizer.from_pretrained(MODEL_PATH)
                self.model = BertForSequenceClassification.from_pretrained(MODEL_PATH)
            else:
                logger.info("Loading pre-trained model: {} (will use rule engine as fallback)", MODEL_NAME)
                self.tokenizer = BertTokenizer.from_pretrained(MODEL_NAME)
                from transformers import BertConfig
                num_labels = len(self.label_map)
                config = BertConfig.from_pretrained(MODEL_NAME, num_labels=num_labels)
                self.model = BertForSequenceClassification.from_pretrained(
                    MODEL_NAME, config=config, ignore_mismatched_sizes=True
                )

            self.model.to(self.device)
            self.model.eval()
            self.loaded = True
            logger.info("BERT model loaded successfully on {}", self.device)
        except Exception as e:
            logger.warning("Failed to load BERT model: {}, will use rule engine only", str(e))
            self.loaded = False

    def predict(self, text: str) -> dict:
        if not self.loaded:
            return {"department_code": "other", "confidence": 0.0, "all_scores": []}

        try:
            from transformers import BertTokenizer
            inputs = self.tokenizer(
                text,
                padding=True,
                truncation=True,
                max_length=128,
                return_tensors="pt"
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            with torch.no_grad():
                outputs = self.model(**inputs)
                logits = outputs.logits
                probs = torch.softmax(logits, dim=-1)
                confidence, pred_idx = torch.max(probs, dim=-1)

            confidence_value = confidence.item()
            pred_code = self.id2label.get(pred_idx.item(), "other")

            all_scores = []
            for idx, prob in enumerate(probs[0].cpu().numpy()):
                code = self.id2label.get(idx, "unknown")
                all_scores.append({
                    "department_code": code,
                    "department_name": DEPARTMENT_MAP.get(code, "未知"),
                    "score": round(float(prob), 4)
                })

            all_scores.sort(key=lambda x: x["score"], reverse=True)

            return {
                "department_code": pred_code,
                "department_name": DEPARTMENT_MAP.get(pred_code, "综合协调科"),
                "confidence": round(confidence_value, 4),
                "all_scores": all_scores[:5]
            }
        except Exception as e:
            logger.error("BERT prediction failed: {}", str(e))
            return {"department_code": "other", "confidence": 0.0, "all_scores": []}

    def train(self, records: list, epochs: int = 3, batch_size: int = 16, learning_rate: float = 2e-5) -> dict:
        if not self.loaded:
            raise RuntimeError("Model not loaded, cannot train")

        from torch.utils.data import Dataset, DataLoader
        from transformers import BertTokenizer, get_linear_schedule_with_warmup

        class DispatchDataset(Dataset):
            def __init__(self, records, tokenizer, label2id, max_length=128):
                self.records = records
                self.tokenizer = tokenizer
                self.label2id = label2id
                self.max_length = max_length

            def __len__(self):
                return len(self.records)

            def __getitem__(self, idx):
                r = self.records[idx]
                text = f"{r.get('title', '')} {r.get('description', '')}"
                label_code = r.get("dept_code", "other")
                label_id = self.label2id.get(label_code, self.label2id["other"])

                encoding = self.tokenizer(
                    text,
                    max_length=self.max_length,
                    padding="max_length",
                    truncation=True,
                    return_tensors="pt"
                )
                return {
                    "input_ids": encoding["input_ids"].squeeze(),
                    "attention_mask": encoding["attention_mask"].squeeze(),
                    "labels": torch.tensor(label_id, dtype=torch.long)
                }

        dataset = DispatchDataset(records, self.tokenizer, self.label2id)
        dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

        self.model.train()
        optimizer = torch.optim.AdamW(self.model.parameters(), lr=learning_rate)
        total_steps = len(dataloader) * epochs
        scheduler = get_linear_schedule_with_warmup(
            optimizer, num_warmup_steps=0, num_training_steps=total_steps
        )

        total_loss = 0
        for epoch in range(epochs):
            epoch_loss = 0
            for batch in dataloader:
                optimizer.zero_grad()
                input_ids = batch["input_ids"].to(self.device)
                attention_mask = batch["attention_mask"].to(self.device)
                labels = batch["labels"].to(self.device)

                outputs = self.model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
                loss = outputs.loss
                loss.backward()
                torch.nn.utils.clip_grad_norm_(self.model.parameters(), 1.0)
                optimizer.step()
                scheduler.step()
                epoch_loss += loss.item()

            avg_loss = epoch_loss / len(dataloader)
            total_loss += epoch_loss
            logger.info("Epoch {}/{} - Loss: {:.4f}", epoch + 1, epochs, avg_loss)

        self.model.eval()
        os.makedirs(MODEL_PATH, exist_ok=True)
        self.model.save_pretrained(MODEL_PATH)
        self.tokenizer.save_pretrained(MODEL_PATH)
        logger.info("Model saved to {}", MODEL_PATH)

        return {
            "total_loss": round(total_loss, 4),
            "avg_loss": round(total_loss / epochs, 4),
            "epochs": epochs,
            "samples": len(records)
        }
