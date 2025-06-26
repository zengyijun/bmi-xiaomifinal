package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zengyijun
 */
@Data
@TableName("voltage_rule")
public class VoltageRuleDO implements RulesDO {
    private Long id;
    private Integer warnId;
    private String warnName;
    private String batteryType;
    private String rule;
    private Integer warnLevel;
    private Integer delFlag;

}
