package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zengyijun
 * 数据库基础类，主要是车架基础信息
 */

@Data
@TableName("car_info")
public class CarFrameDO {
    @TableId("car_id")
    private Integer carId;
    private String batteryType;
}
