package com.miproject.finalwork.mq.schedule;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.common.constant.RedisCacheConstant;
import com.miproject.finalwork.common.context.RuleContext;
import com.miproject.finalwork.common.convention.exception.RemoteException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.BatteryStatusMapper;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VehicleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.WarnMsgMQReqDTO;
import com.miproject.finalwork.service.RuleEvaluationService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 获取数据库中车辆上传的数据，检查其是否存在异常
 * 将异常数据上报给消费者
 * 验证上报数据的正确性
 * @author zengyijun
 */

@Slf4j
@Component
public class DataReport {

    @Autowired
    private BatteryStatusMapper batteryStatusMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;
    
    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void reportData(){
        var lock = redissonClient.getLock(RedisCacheConstant.LOCK_STATUS_UPLOAD_KEY);
        try{
            lock.lock();
            LambdaQueryWrapper<BatteryStatusDO> queryWrapper = Wrappers.lambdaQuery(BatteryStatusDO.class)
                    .eq(BatteryStatusDO::getDelFlag, 0);
            List<BatteryStatusDO> statusDOList = batteryStatusMapper.selectList(queryWrapper);

            for(BatteryStatusDO statusDO : statusDOList){
                VehicleDO vehicle = vehicleMapper.selectById(statusDO.getVid());
                LambdaQueryWrapper<VoltageRuleDO> voltage = Wrappers.lambdaQuery(VoltageRuleDO.class)
                        .eq(VoltageRuleDO::getBatteryType, vehicle.getBatteryType());
                List<BaseRuleDO> v_rule = new ArrayList<>(voltageRuleMapper.selectList(voltage));
                WarnMsgMQReqDTO warnMsgMQReqDTO = isWarnable(v_rule, statusDO);
                if(warnMsgMQReqDTO != null){
                    String topicName = "warn-topic";
                    warnMsgMQReqDTO.setTimeStamp(new Date());
                    byte[] body = JSON.toJSONString(warnMsgMQReqDTO).getBytes(StandardCharsets.UTF_8);
                    rocketMQTemplate.convertAndSend(topicName, body);
                }
                warnMsgMQReqDTO = null;
                LambdaQueryWrapper<CurrentRuleDO> current = Wrappers.lambdaQuery(CurrentRuleDO.class)
                        .eq(CurrentRuleDO::getBatteryType, vehicle.getBatteryType());
                List<BaseRuleDO> c_rule = new ArrayList<>(currentRuleMapper.selectList(current));
                warnMsgMQReqDTO = isWarnable(c_rule, statusDO);
                if(warnMsgMQReqDTO != null){
                    String topicName = "warn-topic";
                    warnMsgMQReqDTO.setTimeStamp(new Date());
                    byte[] body = JSON.toJSONString(warnMsgMQReqDTO).getBytes(StandardCharsets.UTF_8);
                    SendResult result = rocketMQTemplate.syncSend(topicName, body);
                    log.info("数据上传了消息到MQ，状态："+result.getSendStatus());
                }
                statusDO.setDelFlag(1);
                batteryStatusMapper.updateById(statusDO);
            }
        }finally {
            lock.unlock();
        }
    }

    public WarnMsgMQReqDTO isWarnable(List<BaseRuleDO>rules, BatteryStatusDO status){
        for(BaseRuleDO rule: rules){
            try {
                // 创建规则上下文
                RuleContext context = new RuleContext();
                context.setMx(status.getRawMaxVal());
                context.setMn(status.getRawMinVal());

                context.setIx(status.getRawMaxVal());
                context.setIn(status.getRawMinVal());
                
                // 使用规则解析服务评估数据是否符合规则
                boolean res = ruleEvaluationService.evaluateRule(rule.getRule(), context);
                
                if(res && rule.getWarnLevel() >= 0){
                    WarnMsgMQReqDTO reqDTO = new WarnMsgMQReqDTO();
                    BeanUtils.copyProperties(rule, reqDTO);
                    reqDTO.setVid(status.getVid());
                    return reqDTO;
                }
            } catch (Exception e) {
                throw new RemoteException("[规则解析出错]" + e.getMessage());
            }
        }
        return null;
    }

}
