package com.miproject.finalwork.common.constant;

public class RedisCacheConstant {
//    上传数据的分布式锁
//    上传电池状态信息的分布式锁
    public static final String LOCK_STATUS_UPLOAD_KEY = "dmi:lock_status_upload:";
//    新增电压相关规则的分布式锁
    public static final String LOCK_VOLTAGE_UPLOAD_KEY= "dmi:lock_voltage_upload:";
//    新增电流相关规则的分布式锁
    public static final String LOCK_CURRENT_UPLOAD_KEY= "dmi:lock_voltage_upload:";

    public static final String BATTERY_STATUS = "dmi:battery_status:";
}
