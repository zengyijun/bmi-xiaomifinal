package com.miproject.finalwork.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HandlerChainContext {
    private Iterator<HandlerWrapper> chainIterator;
    private Map<String, Object> dataBucket = new HashMap<>();
    private String nextHandlerName;

    public void next(String funcall, Object input) {
        if (chainIterator != null && chainIterator.hasNext()) {
            HandlerWrapper next = chainIterator.next();

            nextHandlerName = next.getHandler().getClass().getSimpleName();
            next.invoke(funcall, input, this);
        }
    }

    void setChainIterator(Iterator<HandlerWrapper> it) {
        this.chainIterator = it;
    }

    public String getNextHandlerName() {
        return nextHandlerName;
    }

    public void put(String key, Object value) { dataBucket.put(key, value); }
    public Object get(String key) { return dataBucket.get(key); }
}