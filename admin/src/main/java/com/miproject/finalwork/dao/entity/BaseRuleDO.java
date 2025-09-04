package com.miproject.finalwork.dao.entity;

import lombok.Data;


public interface BaseRuleDO {
    String getRule();
    Integer getWarnId();
    String getWarnName();
    Integer getWarnLevel();
    String getBatteryType();
}
