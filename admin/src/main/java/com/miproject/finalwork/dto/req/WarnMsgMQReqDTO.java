package com.miproject.finalwork.dto.req;

import lombok.Data;

import java.util.Date;

@Data
public class WarnMsgMQReqDTO {
    private String vid;
    private Integer warnId;
    private Integer warnName;
    private Integer warnLevel;
    private Date timeStamp;
}
