package com.miproject.finalwork.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("warn_info")
public class WarnInfoDO {
    private Long id;
    private String vid;
    private Integer warnId;
    private Integer warnName;
    private Integer warnLevel;
    private Date timeStamp;
}
