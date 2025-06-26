package com.miproject.finalwork.dao.entity;

import lombok.Data;

@Data
public class WarnDetailDO {
    private Integer carId;
    private String batteryType;
    private Integer warnId;
    private String warnName;
    private Integer warnLevel;
}
