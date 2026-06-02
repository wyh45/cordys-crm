UPDATE health_archive SET archive_no = CONCAT('AR-', id) WHERE archive_no IS NULL;
