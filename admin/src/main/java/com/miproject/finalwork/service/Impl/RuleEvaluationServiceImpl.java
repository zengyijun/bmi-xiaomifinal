package com.miproject.finalwork.service.Impl;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.miproject.finalwork.common.context.RuleContext;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.service.RuleEvaluationService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则解析服务类
 * 用于解析和执行各种规则表达式
 */
@Service
public class RuleEvaluationServiceImpl implements RuleEvaluationService {


    /**
     * 使用规则上下文评估规则表达式
     *
     * @param rule 规则表达式字符串
     * @param context 规则上下文
     * @return 评估结果
     * @throws ServiceException 当规则表达式解析失败时抛出
     */
    @Override
    public boolean evaluateRule(String rule, RuleContext context) throws ServiceException {
        try {
            // 将规则表达式转换为Aviator格式
            String expression = convertRuleToAviatorFormat(rule);

            // 使用Aviator表达式引擎计算结果
            Expression compiledExp = AviatorEvaluator.compile(expression);
            Map<String, Object> env = context.toVariableMap();
            Boolean result = (Boolean) compiledExp.execute(env);
            if(result == null)
                throw new ServiceException("规则表达式执行出错");
            return result;
        } catch (Exception e) {
            throw new ServiceException("规则表达式解析错误: " + e.getMessage());
        }
    }
    
    /**
     * 将规则表达式转换为Aviator格式
     * 
     * @param rule 原始规则表达式
     * @return 转换后的Aviator格式表达式
     */
    private String convertRuleToAviatorFormat(String rule) {
        String expression = rule;
        
        // 处理各种区间表达式
        if(rule.contains("Mx")) {
            expression = convertIntervalExpression(expression, "Mx", "Mn");
        }
        else if(rule.contains("Ix")) {
            expression = convertIntervalExpression(expression, "Ix", "In");
        }
        return expression;
    }

    @Override
    public boolean checkRule(String rule){
        return true;
    }

    /**
     * 转换区间表达式（处理如 Mx-Mn 的情况）
     * 
     * @param expression 原始表达式
     * @param maxVar 最大值变量名
     * @param minVar 最小值变量名
     * @return 转换后的表达式
     */
    private String convertIntervalExpression(String expression, String maxVar, String minVar) {
        // 处理 "10 <= (Mx-Mn) < 20" 这样的表达式
        String pattern = "(\\d+(?:\\.\\d+)?)\\s*<=\\s*\\(\\s*" + maxVar + "\\s*-\\s*" + minVar + "\\s*\\)\\s*<\\s*(\\d+(?:\\.\\d+)?)";
        expression = expression.replaceAll(pattern, "$1 <= (" + maxVar + " - " + minVar + ") && (" + maxVar + " - " + minVar + ") < $2");

        // 10 < (Mx-Mn) <= 20
        pattern = "(\\d+(?:\\.\\d+)?)\\s*<\\s*\\(\\s*" + maxVar + "\\s*-\\s*" + minVar + "\\s*\\)\\s*<=\\s*(\\d+(?:\\.\\d+)?)";
        expression = expression.replaceAll(pattern, "($1 < (" + maxVar + " - " + minVar + ") && (" + maxVar + " - " + minVar + ") <= $2)");

        // 10 < (Mx-Mn) < 20
        pattern = "(\\d+(?:\\.\\d+)?)\\s*<\\s*\\(\\s*" + maxVar + "\\s*-\\s*" + minVar + "\\s*\\)\\s*<\\s*(\\d+(?:\\.\\d+)?)";
        expression = expression.replaceAll(pattern, "$1 < (" + maxVar + " - " + minVar + ") && (" + maxVar + " - " + minVar + ") < $2");

        // 10 <= (Mx-Mn) <= 20
        pattern = "(\\d+(?:\\.\\d+)?)\\s*<=\\s*\\(\\s*" + maxVar + "\\s*-\\s*" + minVar + "\\s*\\)\\s*<=\\s*(\\d+(?:\\.\\d+)?)";
        expression = expression.replaceAll(pattern, "$1 <= (" + maxVar + " - " + minVar + ") && (" + maxVar + " - " + minVar + ") <= $2");



        return expression;
    }
}

