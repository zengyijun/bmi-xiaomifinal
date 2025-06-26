package com.miproject.finalwork.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miproject.finalwork.dao.entity.WarnInfoDO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarnInfoMapper extends BaseMapper<WarnInfoDO> {
    List<WarnInfoRespDTO> getWarnInfo(String vid);
}
