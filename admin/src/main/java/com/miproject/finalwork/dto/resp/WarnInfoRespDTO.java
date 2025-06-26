package com.miproject.finalwork.dto.resp;

import lombok.Data;

import java.util.Date;

@Data
public class WarnInfoRespDTO {
    private String vid;
    private Integer carId;
    private String batteryType;
    private Integer warnId;
    private String warnName;
    private Integer warnLevel;
    private Date timeStamp;
}
