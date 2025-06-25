package com.miproject.finalwork.dto.req;

import lombok.Data;

/**
 * @author zengyijun
 */
@Data
public class WarnReqDTO {
    private Integer carId;
    private Integer warnId;
    private String signal;
}
