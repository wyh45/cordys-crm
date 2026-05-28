CREATE TABLE health_ai_interpret_record (
    id VARCHAR(64) NOT NULL COMMENT '主键',
    customer_id VARCHAR(64) DEFAULT NULL COMMENT '客户ID',
    archive_id VARCHAR(64) DEFAULT NULL COMMENT '档案ID',
    customer_name VARCHAR(128) DEFAULT NULL COMMENT '客户姓名',
    suggestion_type VARCHAR(64) DEFAULT NULL COMMENT '建议类型: 检查建议/生活方式建议/饮食建议/运动建议',
    interpretation LONGTEXT DEFAULT NULL COMMENT '解读内容（AI返回的完整文本）',
    push_content LONGTEXT DEFAULT NULL COMMENT '推送内容（发送给客户的摘要）',
    push_channel VARCHAR(32) DEFAULT NULL COMMENT '推送渠道',
    interpret_time BIGINT DEFAULT NULL COMMENT '解释时间（时间戳）',
    create_user VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    update_user VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    create_time BIGINT DEFAULT NULL COMMENT '创建时间',
    update_time BIGINT DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_archive_id (archive_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_interpret_time (interpret_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI健康解读记录表';
