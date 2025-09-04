package com.miproject.finalwork.service.Impl;

import com.alibaba.fastjson.JSON;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.mapper.WarnInfoMapper;
import com.miproject.finalwork.dto.req.WarnInfoQueryReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoPageRespDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.service.WarnInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WarnInfoServiceImpl implements WarnInfoService {

    @Autowired
    private WarnInfoMapper warnInfoMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<WarnInfoRespDTO> getWarnInfo(String vid) {
        // 先从缓存中获取
        String cacheKey = "warn_info:" + vid;
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cachedValue != null) {
            // 缓存命中，直接返回
            return JSON.parseArray(cachedValue, WarnInfoRespDTO.class);
        }
        
        // 缓存未命中，查询数据库
        List<WarnInfoRespDTO> resp = warnInfoMapper.getWarnInfo(vid);
        if(resp == null){
            throw new ServiceException(BaseErrorCode.DATA_ERROR);
        }
        
        // 将结果存入缓存，设置过期时间5分钟
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(resp), 5, TimeUnit.MINUTES);
        
        return resp;
    }

    @Override
    public WarnInfoPageRespDTO getWarnInfoByPage(WarnInfoQueryReqDTO queryReqDTO) {
        String vid = queryReqDTO.getVid();
        int pageNum = queryReqDTO.getPageNum();
        int pageSize = queryReqDTO.getPageSize();
        
        // 构造缓存key
        String cacheKey = String.format("warn_info_page:%s:%d:%d", vid, pageNum, pageSize);
        
        // 先从缓存中获取
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            // 缓存命中，直接返回
            return JSON.parseObject(cachedValue, WarnInfoPageRespDTO.class);
        }
        
        // 计算分页参数
        int offset = (pageNum - 1) * pageSize;
        
        // 查询数据
        List<WarnInfoRespDTO> records = warnInfoMapper.getWarnInfoByPage(vid, offset, pageSize);
        
        // 查询总数
        Long total = warnInfoMapper.getWarnInfoCount(vid);
        
        // 构造分页结果
        WarnInfoPageRespDTO pageRespDTO = new WarnInfoPageRespDTO();
        pageRespDTO.setRecords(records);
        pageRespDTO.setTotal(total);
        pageRespDTO.setPageNum(pageNum);
        pageRespDTO.setPageSize(pageSize);
        pageRespDTO.setPages((int) Math.ceil((double) total / pageSize));
        pageRespDTO.setHasNext(pageNum < pageRespDTO.getPages());
        pageRespDTO.setHasPrevious(pageNum > 1);
        
        // 将结果存入缓存，设置过期时间5分钟
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(pageRespDTO), 5, TimeUnit.MINUTES);
        
        return pageRespDTO;
    }
}