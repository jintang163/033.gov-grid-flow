import os

CONFIDENCE_THRESHOLD = float(os.getenv("CONFIDENCE_THRESHOLD", "0.8"))
MODEL_NAME = os.getenv("MODEL_NAME", "bert-base-chinese")
MODEL_PATH = os.getenv("MODEL_PATH", "./model")
NLP_SERVICE_HOST = os.getenv("NLP_SERVICE_HOST", "0.0.0.0")
NLP_SERVICE_PORT = int(os.getenv("NLP_SERVICE_PORT", "8001"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

DEPARTMENT_MAP = {
    "municipal": "市政科",
    "environmental": "环卫科",
    "law_enforcement": "综合执法大队",
    "traffic": "交通科",
    "safety": "安监科",
    "civil_affairs": "民政科",
    "public_security": "治安科",
    "urban_management": "城管科",
    "water": "水务科",
    "greening": "园林绿化科",
    "housing": "住建科",
    "power": "电力科",
    "other": "综合协调科"
}

EVENT_TYPE_DEPT_RULES = {
    "environment": "environmental",
    "public_facility": "municipal",
    "dispute": "civil_affairs",
    "safety_hazard": "safety",
    "security": "public_security",
    "service": "civil_affairs",
    "traffic": "traffic",
    "other": "other"
}

KEYWORD_RULES = [
    (["井盖", "路灯", "护栏", "路面", "道板", "市政设施", "公共设施", "消防栓", "信号灯"], "municipal"),
    (["垃圾", "保洁", "清扫", "卫生", "异味", "污水", "粪池", "公厕", "化粪池", "环境"], "environmental"),
    (["违建", "违搭", "占道", "摆摊", "广告牌", "噪音", "油烟", "城管"], "urban_management"),
    (["交通", "拥堵", "违停", "停车", "红绿灯", "斑马线", "路障"], "traffic"),
    (["火灾", "煤气", "燃气", "危房", "化学品", "爆炸", "漏电", "安全"], "safety"),
    (["偷盗", "抢劫", "打架", "赌博", "传销", "诈骗", "治安"], "public_security"),
    (["低保", "养老", "救助", "残疾", "扶贫", "民生"], "civil_affairs"),
    (["水管", "漏水", "停水", "水压", "排水", "积水", "水务"], "water"),
    (["绿化", "树木", "花草", "公园", "绿地"], "greening"),
    (["房屋", "裂缝", "渗水", "物业", "拆迁", "安置", "住房"], "housing"),
    (["停电", "电线", "变压器", "电压"], "power"),
]
