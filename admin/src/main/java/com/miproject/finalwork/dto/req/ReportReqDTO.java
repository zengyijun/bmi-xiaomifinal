package com.miproject.finalwork.dto.req;

import lombok.Data;

/**
 * @author zengyijun
 */
@Data
public class ReportReqDTO {
    private String vid;
    private Float rawMaxVal;
    private Float rawMinVal;
    private String unit;
    private Integer type;
    // 0代表不带数据，1代表自带数据
    // 10代表申请详细数据，11代表自带数据申请详细数据
    private Integer reqType;
}
