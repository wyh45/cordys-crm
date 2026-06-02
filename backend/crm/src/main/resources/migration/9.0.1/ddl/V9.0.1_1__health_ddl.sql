-- 健康档案
CREATE TABLE IF NOT EXISTS health_archive (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    customer_name VARCHAR(100),
    gender VARCHAR(10),
    age INT,
    blood_type VARCHAR(10),
    height DECIMAL(5,1),
    weight DECIMAL(5,1),
    phone VARCHAR(20),
    idcard_no VARCHAR(20),
    archive_no VARCHAR(50),
    blood_pressure VARCHAR(20),
    heart_rate INT,
    risk_score INT,
    smoking VARCHAR(20),
    drinking VARCHAR(20),
    sleep_quality VARCHAR(20),
    exercise VARCHAR(20),
    diet VARCHAR(20),
    blood_sugar DECIMAL(5,2),
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 过敏史
CREATE TABLE IF NOT EXISTS health_allergy (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    allergen VARCHAR(200),
    severity VARCHAR(20),
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 体检档案映射
CREATE TABLE IF NOT EXISTS health_archive_mapping (
    id VARCHAR(64) PRIMARY KEY,
    idcard_no VARCHAR(20),
    customer_id VARCHAR(64),
    archive_id VARCHAR(64),
    exam_no VARCHAR(64),
    exam_date BIGINT,
    sync_status INT DEFAULT 0,
    error_msg VARCHAR(500),
    create_time BIGINT,
    update_time BIGINT
);

-- 病史
CREATE TABLE IF NOT EXISTS health_medical_history (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    disease_name VARCHAR(200),
    diagnosis_date BIGINT,
    treatment_status VARCHAR(50),
    family_history VARCHAR(500),
    remarks VARCHAR(500),
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 疫苗接种
CREATE TABLE IF NOT EXISTS health_vaccination (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    vaccine_name VARCHAR(200),
    vaccinate_date BIGINT,
    next_dose_date BIGINT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 体检记录
CREATE TABLE IF NOT EXISTS health_examination (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    exam_no VARCHAR(64),
    exam_date BIGINT,
    exam_item VARCHAR(200),
    result_value VARCHAR(200),
    reference_range VARCHAR(200),
    is_abnormal TINYINT(1) DEFAULT 0,
    result_flag VARCHAR(20),
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- AI解读记录
CREATE TABLE IF NOT EXISTS health_ai_interpret_record (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    archive_id VARCHAR(64),
    customer_name VARCHAR(100),
    interpretation TEXT,
    push_content TEXT,
    push_channel VARCHAR(50),
    suggestion_type VARCHAR(100),
    interpret_time BIGINT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 健康知识库
CREATE TABLE IF NOT EXISTS health_knowledge (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(200),
    content TEXT,
    tags VARCHAR(500),
    category VARCHAR(100),
    embedding TEXT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 随访规则模板
CREATE TABLE IF NOT EXISTS health_follow_rule (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200),
    description VARCHAR(500),
    condition_type VARCHAR(50),
    condition_expr TEXT,
    follow_type VARCHAR(50),
    follow_result_template TEXT,
    follow_interval_days INT,
    enabled TINYINT(1) DEFAULT 1,
    priority INT DEFAULT 0,
    watch_exam_items VARCHAR(500),
    min_abnormal_count INT,
    follow_method VARCHAR(50),
    follow_interval INT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 随访记录
CREATE TABLE IF NOT EXISTS health_follow_record (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    follow_date BIGINT,
    follow_type VARCHAR(50),
    follow_result TEXT,
    next_follow_date BIGINT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);

-- 健康推送记录
CREATE TABLE IF NOT EXISTS health_push_record (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    knowledge_id VARCHAR(64),
    push_channel VARCHAR(50),
    push_status VARCHAR(20),
    push_time BIGINT,
    create_user VARCHAR(64),
    update_user VARCHAR(64),
    create_time BIGINT,
    update_time BIGINT
);
