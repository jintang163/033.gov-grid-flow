import os

CONFIDENCE_THRESHOLD = float(os.getenv("CONFIDENCE_THRESHOLD", "0.8"))
MODEL_NAME = os.getenv("MODEL_NAME", "bert-base-chinese")
MODEL_PATH = os.getenv("MODEL_PATH", "./model")
NLP_SERVICE_HOST = os.getenv("NLP_SERVICE_HOST", "0.0.0.0")
NLP_SERVICE_PORT = int(os.getenv("NLP_SERVICE_PORT", "8001"))
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

DEPARTMENT_MAP = {
    "MUNICIPAL": "市政科",
    "ENVIRONMENTAL": "环卫科",
    "LAW_ENFORCEMENT": "综合执法大队",
    "TRAFFIC": "交通科",
    "SAFETY": "安监科",
    "CIVIL_AFFAIRS": "民政科",
    "PUBLIC_SECURITY": "治安科",
    "URBAN_MANAGEMENT": "城管科",
    "WATER": "水务科",
    "GREENING": "园林绿化科",
    "HOUSING": "住建科",
    "POWER": "电力科",
    "OTHER": "综合协调科"
}

EVENT_TYPE_DEPT_RULES = {
    "environment": "ENVIRONMENTAL",
    "public_facility": "MUNICIPAL",
    "dispute": "CIVIL_AFFAIRS",
    "safety_hazard": "SAFETY",
    "security": "PUBLIC_SECURITY",
    "service": "CIVIL_AFFAIRS",
    "traffic": "TRAFFIC",
    "other": "OTHER"
}

KEYWORD_RULES = [
    (["井盖", "路灯", "护栏", "路面", "道板", "市政设施", "公共设施", "消防栓", "信号灯"], "MUNICIPAL"),
    (["垃圾", "保洁", "清扫", "卫生", "异味", "污水", "粪池", "公厕", "化粪池", "环境"], "ENVIRONMENTAL"),
    (["违建", "违搭", "占道", "摆摊", "广告牌", "噪音", "油烟", "城管"], "URBAN_MANAGEMENT"),
    (["交通", "拥堵", "违停", "停车", "红绿灯", "斑马线", "路障"], "TRAFFIC"),
    (["火灾", "煤气", "燃气", "危房", "化学品", "爆炸", "漏电", "安全"], "SAFETY"),
    (["偷盗", "抢劫", "打架", "赌博", "传销", "诈骗", "治安"], "PUBLIC_SECURITY"),
    (["低保", "养老", "救助", "残疾", "扶贫", "民生"], "CIVIL_AFFAIRS"),
    (["水管", "漏水", "停水", "水压", "排水", "积水", "水务"], "WATER"),
    (["绿化", "树木", "花草", "公园", "绿地"], "GREENING"),
    (["房屋", "裂缝", "渗水", "物业", "拆迁", "安置", "住房"], "HOUSING"),
    (["停电", "电线", "变压器", "电压"], "POWER"),
]
