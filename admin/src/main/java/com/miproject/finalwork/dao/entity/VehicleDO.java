package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zengyijun
 */
@Data
@TableName("vehicle_info")
public class VehicleDO {
    @TableId(value="vid", type= IdType.INPUT)
    private String vid;
    private Integer carId;
    private String batteryType;
    private Integer totalMileage;
    private Integer batteryHealth;
    private Integer delFlag;
}
