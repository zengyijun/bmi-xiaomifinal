package com.miproject.finalwork.dto.req;

import lombok.Data;

@Data
public class WarnInfoQueryReqDTO {
    /**
     * 车辆ID
     */
    private String vid;
    
    /**
     * 页码，从1开始
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小，默认20条
     */
    private Integer pageSize = 20;
    
    /**
     * 最大每页大小限制
     */
    private static final int MAX_PAGE_SIZE = 100;
    
    public Integer getPageSize() {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
    
    public Integer getPageNum() {
        if (pageNum == null || pageNum <= 0) {
            return 1;
        }
        return pageNum;
    }
}