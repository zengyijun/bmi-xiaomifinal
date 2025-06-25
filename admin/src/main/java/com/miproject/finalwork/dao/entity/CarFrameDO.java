package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zengyijun
 * 数据库基础类，主要是车架基础信息
 */

@Data
@TableName("car_info")
public class CarFrameDO {
    private int carId;
    private String batteryType;
}
