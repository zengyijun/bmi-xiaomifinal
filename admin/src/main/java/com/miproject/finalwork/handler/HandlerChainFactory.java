package com.miproject.finalwork.handler;

import com.miproject.finalwork.handler.annotation.AlarmType;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class HandlerChainFactory implements ApplicationContextAware {
    private final Map<String, List<HandlerWrapper>> registry = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Map<String, Object> beans = context.getBeansWithAnnotation(AlarmType.class);
        for(Object bean:beans.values()){
            AlarmType type = bean.getClass().getAnnotation(AlarmType.class);
            Method handleMethod = Arrays.stream(bean.getClass().getMethods())
                    .filter(m-> "handle".equals(m.getName()))
                    .findFirst().orElseThrow(()->new IllegalStateException("未找到Handle方法"+bean.getClass().getName()));

            registry.computeIfAbsent(type.value(), k->new ArrayList<>())
                    .add(new HandlerWrapper(bean, handleMethod));
        }
    }

    public Object execute(String[] types, String funcall, Object input){
        List<HandlerWrapper> chain = new ArrayList<>();
        for(String type:types){
            List<HandlerWrapper> handlers = registry.get(type);
            if(handlers != null){
                chain.addAll(handlers);
            }
        }
        HandlerChainContext context = new HandlerChainContext();
        context.setChainIterator(chain.iterator());
        context.next(funcall, input);
        return context.get("finalResult");

    }


}
