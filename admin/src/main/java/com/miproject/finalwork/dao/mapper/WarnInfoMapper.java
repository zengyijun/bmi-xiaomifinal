package com.miproject.finalwork.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miproject.finalwork.dao.entity.WarnInfoDO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarnInfoMapper extends BaseMapper<WarnInfoDO> {
    List<WarnInfoRespDTO> getWarnInfo(String vid);
    
    /**
     * 分页查询告警信息
     * @param vid 车辆ID
     * @param offset 偏移量
     * @param limit 限制条数
     * @return 告警信息列表
     */
    List<WarnInfoRespDTO> getWarnInfoByPage(@Param("vid") String vid, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 查询告警信息总数
     * @param vid 车辆ID
     * @return 总数
     */
    Long getWarnInfoCount(@Param("vid") String vid);
}