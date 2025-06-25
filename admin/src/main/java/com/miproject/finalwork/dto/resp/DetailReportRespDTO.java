package com.miproject.finalwork.dto.resp;

import lombok.Data;

/**
 * @author zengyijun
 */
@Data
public class DetailReportRespDTO {
    private String vid;
    private int carId;
    private String batteryType;
    private int warnId;
    private String warnName;
    private Float rawMaxVal;
    private Float rawMinVal;
    private int warnLevel;
}
