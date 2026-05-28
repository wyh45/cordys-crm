ALTER TABLE health_follow_rule
    ADD COLUMN watch_exam_items VARCHAR(500) NULL COMMENT '关注的异常检查项，逗号分隔',
    ADD COLUMN min_abnormal_count INT DEFAULT 1 COMMENT '最小异常项数量，达到此值触发规则',
    ADD COLUMN follow_method VARCHAR(50) NULL COMMENT '随访方式: SMS/PHONE/VISIT/WECHAT',
    ADD COLUMN follow_interval INT DEFAULT 7 COMMENT '随访间隔天数';
