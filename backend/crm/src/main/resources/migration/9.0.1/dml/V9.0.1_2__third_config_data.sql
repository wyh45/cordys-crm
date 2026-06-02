-- 初始化默认第三方配置（避免SQLBOT等接口报配置不存在错误）
INSERT IGNORE INTO sys_organization_config (id, type, organization_id, sync, sync_resource, enable, create_time, update_time, create_user, update_user)
VALUES ('third-default', 'THIRD', '100001', 0, 'WECOM', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

INSERT IGNORE INTO sys_organization_config_detail (id, config_id, type, enable, name, content, create_time, update_time, create_user, update_user)
VALUES ('sqlbot-chat-detail', 'third-default', 'SQLBOT_CHAT', 0, 'third.setting', '{"type":"SQLBOT","verify":false,"config":null}', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');
