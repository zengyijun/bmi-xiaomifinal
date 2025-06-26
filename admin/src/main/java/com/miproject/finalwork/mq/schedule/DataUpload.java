package com.miproject.finalwork.mq.schedule;

import cn.hutool.core.lang.hash.Hash;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.dao.entity.BatteryStatusDO;
import com.miproject.finalwork.dao.entity.CurrentRuleDO;
import com.miproject.finalwork.dao.entity.VoltageRuleDO;
import com.miproject.finalwork.dao.mapper.BatteryStatusMapper;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 自动上报电池信息
// 定时根据规则生辰数据上传
@Slf4j
@Component
public class DataUpload {
    private final String[] vids = new String[]{"bE7yHdL6rWiO3xK9", "tGz8wQe93kL1VaBs", "X2mF0uBQaPcN7ZrL"};
    private final String[] batteryTypes = new String[]{"三元电池", "铁锂电池", "三元电池"};
    private static final String url = "http://127.0.0.1:9000/api/reportData";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

    @Scheduled(cron = "0 */2 * * * ?")
    public void dataUpload(){
        int ts = (int) (System.currentTimeMillis() / 1000);
        int vid = ts % 3;
        int ruleId = ts % 2 + 1;
        String rule;
        if(ruleId == 1){
            rule = getVoltageRule(batteryTypes[vid]);
        }
        else{
            rule = getCurrentRule(batteryTypes[vid]);
        }
        Float min, max;
        Float[] res= genData(rule);
        min = res[0];
        max = res[1];

        ReportReqDTO reportReqDTO = new ReportReqDTO();
        reportReqDTO.setVid(vids[vid]);
        reportReqDTO.setType(ruleId);
        reportReqDTO.setRawMinVal(min);
        reportReqDTO.setRawMaxVal(max);
        reportReqDTO.setReqType(1);
        reportReqDTO.setUnit("xx");

        try{
            ResponseEntity<String> resp = restTemplate.postForEntity(url, reportReqDTO, String.class);

        }catch(Exception e){
            log.error("[数据上传服务存在错误]："+e.getMessage());
        }



    }

    public static Float[] genData(String rule) {
        List<Float> nums = new ArrayList<>();
        // 修正正则：提取 float 数字（可含小数点）
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(rule);
        while (matcher.find()) {
            nums.add(Float.parseFloat(matcher.group()));
        }

        float min = 0f;
        float max = 0f;

        if (nums.size() == 1) {
            float range = nums.get(0);
            min = getRandomFloat(0, 100);
            max = min + range;
        } else if (nums.size() == 2) {
            float low = Math.min(nums.get(0), nums.get(1));
            float high = Math.max(nums.get(0), nums.get(1));

            float base = getRandomFloat(0, 100);
            min = base;
            max = getRandomFloat(base + low, base + high);
        } else {
            throw new IllegalArgumentException("规则中未检测到合法数字");
        }

        return new Float[] { min, max }; // 你可以自己决定是 [Mx, Mi] 还是 [val1, val2]
    }
    private static float getRandomFloat(float min, float max) {
        Random random = new Random();
        return min + random.nextFloat() * (max - min);
    }

    public String getVoltageRule(String batteryType){
        LambdaQueryWrapper<VoltageRuleDO> wrapper = Wrappers.lambdaQuery(VoltageRuleDO.class)
                .eq(VoltageRuleDO::getBatteryType, batteryType);
        List<VoltageRuleDO> rules = voltageRuleMapper.selectList(wrapper);

        int size = rules.size();
        int index = ThreadLocalRandom.current().nextInt(0, size);
        return rules.get(index).getRule();
    }
    public String getCurrentRule(String batteryType){
        LambdaQueryWrapper<CurrentRuleDO> wrapper = Wrappers.lambdaQuery(CurrentRuleDO.class)
                .eq(CurrentRuleDO::getBatteryType, batteryType);
        List<CurrentRuleDO> rules = currentRuleMapper.selectList(wrapper);

        int size = rules.size();
        int index = ThreadLocalRandom.current().nextInt(0, size);
        return rules.get(index).getRule();
    }

}
