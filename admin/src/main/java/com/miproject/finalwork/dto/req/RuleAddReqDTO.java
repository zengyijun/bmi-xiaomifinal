package com.miproject.finalwork.dto.req;

import lombok.Data;

@Data
public class RuleAddReqDTO {
    private Integer warnId;
    private String warnName;
    private String batteryType;
//    rule中val标识比较的值
    private String rule;
    private Integer type;
    private Integer warnLevel;
}
