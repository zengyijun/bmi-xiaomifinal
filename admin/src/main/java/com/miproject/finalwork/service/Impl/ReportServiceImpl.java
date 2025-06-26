package com.miproject.finalwork.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.BatteryStatusMapper;

import com.miproject.finalwork.dto.req.ReportReqDTO;

import com.miproject.finalwork.dto.resp.ReportRespDTO;

import com.miproject.finalwork.service.ReportService;

import lombok.var;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import java.util.Date;


import static com.miproject.finalwork.common.constant.RedisCacheConstant.BATTERY_STATUS;
import static com.miproject.finalwork.common.constant.RedisCacheConstant.LOCK_STATUS_UPLOAD_KEY;

/**
 * @author zengyijun
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private BatteryStatusMapper batteryStatusMapper;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;



    public boolean hasData(ReportReqDTO reqDTO){

        if(reqDTO.getReqType() == 1 || reqDTO.getReqType() == 11){
            return (reqDTO.getRawMaxVal() != null || reqDTO.getRawMinVal() != null);
        }
        return false;
    }

    /**
     * 上传电池状态
     * 分两个步骤完成：将旧的同类型的删除（del_flag置为1），将新的插入到表
     * 实现思路：并发锁获取访问数据库权限；
     * 实现双写并设置redis的过期时间；
     * @param reqDTO（vid、rawMaxVal、rawMinVal、unit、type、reqType）
     */
    @Override
    public void uploadStatus(ReportReqDTO reqDTO) {
        if(!hasData(reqDTO)){
            throw new ClientException(BaseErrorCode.DATA_ERROR);
        }
        var lock = redissonClient.getLock(LOCK_STATUS_UPLOAD_KEY+reqDTO.getVid());
        try{
            lock.lock();
            BatteryStatusDO batteryStatusDO = new BatteryStatusDO();
            BeanUtils.copyProperties(reqDTO, batteryStatusDO);
            batteryStatusDO.setTimestamp(new Date());
//            插入Redis中
            String key = BATTERY_STATUS + reqDTO.getVid() +":"+ reqDTO.getType();
            String value = objectMapper.writeValueAsString(reqDTO);
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofDays(1));
            int i = batteryStatusMapper.insert(batteryStatusDO);
            if(i <= 0){
                throw new ServiceException(BaseErrorCode.SERVICE_ERROR);
            }
        } catch (JsonProcessingException e) {
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR);
        } finally{
            lock.unlock();
        }
    }

    @Override
    public ReportRespDTO getStatus(ReportReqDTO reqDTO) {
        String key = BATTERY_STATUS + reqDTO.getVid() + ":" + reqDTO.getType();
        String value = stringRedisTemplate.opsForValue().get(key);
        if(value != null){
            try {
                return objectMapper.readValue(value, ReportRespDTO.class);
            } catch (JsonProcessingException e) {
                throw new ServiceException(e.getMessage());
            }
        }
//       查数据库表
        LambdaQueryWrapper<BatteryStatusDO> queryWrapper = Wrappers.lambdaQuery(BatteryStatusDO.class)
                .eq(BatteryStatusDO::getVid, reqDTO.getVid())
                .eq(BatteryStatusDO::getType, reqDTO.getType())
                .eq(BatteryStatusDO::getDelFlag, 0);
        BatteryStatusDO batteryStatusDO = batteryStatusMapper.selectOne(queryWrapper);
        ReportRespDTO reportResp = new ReportRespDTO();
        BeanUtils.copyProperties(batteryStatusDO, reportResp);
        try {
            objectMapper.writeValueAsString(reportResp);
        } catch (JsonProcessingException e) {
            throw new ServiceException(e.getMessage());
        }
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofDays(1));
        return reportResp;
    }



}
