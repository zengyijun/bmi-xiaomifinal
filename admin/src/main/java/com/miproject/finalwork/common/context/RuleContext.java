package com.miproject.finalwork.common.context;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 规则执行上下文
 * 用于封装规则计算所需的所有变量
 */
@Data
public class RuleContext {
    // 电压相关变量
    private Float mx; // 最大电压
    private Float mn; // 最小电压

    // 电流相关变量
    private Float ix; // 最大电流
    private Float in; // 最小电流

    // 温度相关变量（预留）
    private Float temperature; // 温度

    // 其他自定义变量
    private Map<String, Object> customVariables = new HashMap<>();

    /**
     * 获取所有变量的Map表示
     * @return 变量Map
     */
    public Map<String, Object> toVariableMap() {
        Map<String, Object> variables = new HashMap<>();

        if (mx != null) variables.put("Mx", mx);
        if (mn != null) variables.put("Mn", mn);
        if (ix != null) variables.put("Ix", ix);
        if (in != null) variables.put("In", in);
        if (temperature != null) variables.put("temperature", temperature);

        variables.putAll(customVariables);

        return variables;
    }

    /**
     * 添加自定义变量
     * @param name 变量名
     * @param value 变量值
     */
    public void addVariable(String name, Object value) {
        customVariables.put(name, value);
    }
}
