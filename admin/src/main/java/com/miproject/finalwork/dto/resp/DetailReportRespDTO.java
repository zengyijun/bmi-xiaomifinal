package com.miproject.finalwork.dto.resp;

import lombok.Data;

/**
 * @author zengyijun
 */
@Data
public class DetailReportRespDTO {
    private String vid;
    private Integer carId;
    private String batteryType;
    private Integer warnId;
    private String warnName;
    private Float rawMaxVal;
    private Float rawMinVal;
    private Integer warnLevel;
}
