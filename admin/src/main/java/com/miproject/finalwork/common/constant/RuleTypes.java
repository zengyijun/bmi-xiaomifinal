package com.miproject.finalwork.common.constant;

public enum RuleTypes {

    VOLTAGE_RULE(1, "voltage"),

    CURRENT_RULE(2, "current"),
    REPO_POST(3, "新增电池状态"),
    REPO_GET(4, "获取电池状态");

    private final int code;
    private final String rule;

    RuleTypes(int code, String rule){
        this.code = code;
        this.rule = rule;
    };

    public int getCode() {
        return code;
    }


    public String getRule(){
        return rule;
    }

    public static String getByCode(int code){
        for(RuleTypes type : RuleTypes.values()){
            if(type.getCode() == code) {
                return type.getRule();
            }
        }
        return null;
    }

}
