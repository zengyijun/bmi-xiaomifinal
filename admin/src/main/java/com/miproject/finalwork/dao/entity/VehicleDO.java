package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zengyijun
 */
@Data
@TableName("vehicle_info")
public class VehicleDO {
    @TableField("vid")
    private String vid;
    private int carId;
    private String batteryType;
    private int totalMileage;
    private int batteryHealth;
    private int delFlag;
}
