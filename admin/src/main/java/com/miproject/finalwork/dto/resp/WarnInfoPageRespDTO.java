package com.miproject.finalwork.dto.resp;

import lombok.Data;

import java.util.List;

@Data
public class WarnInfoPageRespDTO {
    /**
     * 告警信息列表
     */
    private List<WarnInfoRespDTO> records;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 页码
     */
    private Integer pageNum;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer pages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
}