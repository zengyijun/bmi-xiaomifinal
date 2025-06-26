package com.miproject.finalwork.dto.resp;

import lombok.Data;

@Data
public class ReportRespDTO {
    private String vid;
    private Float rawMaxVal;
    private Float rawMinVal;
    private String unit;
    private Integer type;
}
