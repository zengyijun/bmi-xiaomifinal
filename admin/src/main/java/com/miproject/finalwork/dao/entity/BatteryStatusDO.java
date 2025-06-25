package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.util.Date;


/**
 * @author zengyijun
 */
@Data
@TableName("battery_status")
public class BatteryStatusDO {
    private String vid;
    private Float rawMaxVal;
    private Float rawMinVal;
    private String unit;
    private int type;
    private Date timestamp;
    private int delFlag;



}
