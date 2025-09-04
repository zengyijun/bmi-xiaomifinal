-- 优化查询性能的索引

-- 为 vehicle_info 表的 vid 字段添加索引
ALTER TABLE vehicle_info ADD INDEX idx_vid (vid);

-- 为 warn_info 表的 vid 字段添加索引
ALTER TABLE warn_info ADD INDEX idx_vid (vid);

-- 为 warn_info 表的 time_stamp 字段添加索引，便于按时间排序查询
ALTER TABLE warn_info ADD INDEX idx_time_stamp (time_stamp);

-- 为 vehicle_info 表的 battery_type 字段添加索引，便于按电池类型查询
ALTER TABLE vehicle_info ADD INDEX idx_battery_type (battery_type);