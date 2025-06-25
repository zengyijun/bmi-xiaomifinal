package com.miproject.finalwork.dao.entity;

import lombok.Data;

public interface RulesDO {
    String getRule();
    int getWarnId();
    String getWarnName();
    int getWarnLevel();
    String getBatteryType();
}