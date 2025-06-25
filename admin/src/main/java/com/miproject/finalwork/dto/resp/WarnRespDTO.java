package com.miproject.finalwork.dto.resp;

import lombok.Data;

/**
 * @author zengyijun
 */
@Data
public class WarnRespDTO {
    private int carId;
    private String batteryType;
    private String warnName;
    private int warnLevel;
}
