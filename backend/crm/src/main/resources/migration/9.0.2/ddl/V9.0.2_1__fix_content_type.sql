-- 修复 BLOB 列导致 MyBatis 无法正确映射为 String 的问题
ALTER TABLE sys_organization_config_detail MODIFY COLUMN content MEDIUMTEXT;
