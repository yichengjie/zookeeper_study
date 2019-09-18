package com.yicj.study.rpc.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这个主要是模拟spring容器
 */
public class RpcBeanFactory {

    private static Map<String, Object> beanMap = new ConcurrentHashMap<>();

    public static Object getBean(String beanName) {
        return beanMap.get(beanName);
    }

    public static void putBean(String beanName, Object object) {
        beanMap.put(beanName, object);
    }
}