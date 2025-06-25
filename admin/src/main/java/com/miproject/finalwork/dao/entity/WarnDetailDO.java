package com.miproject.finalwork.dao.entity;

import lombok.Data;

@Data
public class WarnDetailDO {
    private int carId;
    private String batteryType;
    private int warnId;
    private String warnName;
    private int warnLevel;
}
