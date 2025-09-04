package com.miproject.finalwork.service.Impl;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.service.DataValidationService;
import com.miproject.finalwork.service.RuleEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class DataValidationServiceImpl implements DataValidationService {

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Override
    public boolean checkWarnData(List<WarnReqDTO> data) {
        Set<String> skip = new HashSet<>();
        skip.add("warnId");
        return checkListData(skip, null, data);
    }

    /**
     * 字段检查
     * 检查规则是否合法
     */
    @Override
    public boolean checkRuleData(RuleAddReqDTO data) {
        Map<String, Object> mustIncudes = new HashMap<>();
        mustIncudes.put("warnName", new String[]{"电压差预警", "电流差预警"});
        mustIncudes.put("batteryType", new String[]{"三元电池", "铁锂电池"});
        mustIncudes.put("rule", new String[]{"<", ">", "=", "val"});
        return checkData(null, mustIncudes, data) && ruleEvaluationService.checkRule(data.getRule());
    }

    @Override
    public boolean checkReportUploadData(ReportReqDTO data) {
        if(data.getVid() == null || data.getVid().isEmpty())
            return false;
        return checkData(null, null, data);
    }

    public boolean checkListData(Set<String> skips, Map<String, Object> mustIncludes, Object datas){
        if(((List)datas).isEmpty() || datas == null ){
            return false;
        }
        for(Object data:((List)datas)){
            boolean status = checkData(skips, mustIncludes, data);
            if(!status)
                return false;
        }
        return true;


    }

    public boolean checkData(Set<String> skips, Map<String, Object> mustInculdes, Object data){
        if(data == null) {
            return false;
        }
        try{
            for(Field field: data.getClass().getDeclaredFields()){
                field.setAccessible(true);
                String name = field.getName();
                if(skips!=null && skips.contains(name)){
                    continue;
                }
                Object value = field.get(data);
                Object sets = null;
                if(mustInculdes != null) {
                    sets = mustInculdes.get(name);

                }
                if(value != null){
                    if(value instanceof String){
                        if(((String) value).isEmpty()) {
                            return false;
                        }
                        if(!(sets == null)) {
                            boolean flag = false;
                            for (String set : (String[]) sets) {
                                if (((String) value).contains(set)) {
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                return false;
                            }
                        }

                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
