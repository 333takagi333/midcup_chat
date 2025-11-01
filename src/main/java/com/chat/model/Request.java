package com.chat.model;

/**
 * 通用请求模型。
 * 封装请求的类型以及对应的负载数据。
 */
public class Request {
    private String type;
    private Object payload;

    /**
     * 构造一个请求对象。
     * @param type 请求类型，例如 "LOGIN"
     * @param payload 请求数据载荷
     */
    public Request(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * 获取请求类型。
     */
    public String getType() {
        return type;
    }

    /**
     * 设置请求类型。
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取请求载荷。
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * 设置请求载荷。
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
