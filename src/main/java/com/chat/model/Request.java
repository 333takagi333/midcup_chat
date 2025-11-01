package com.chat.model;

import java.io.Serializable;

/**
 * 通用请求封装：包含协议 type、数据体 data 以及时间戳。
 */
public class Request implements Serializable {
    private String type;
    private Object data;
    private long timestamp;

    /**
     * 构造一个请求对象。
     * @param type 协议类型（见 com.chat.protocol.MessageType）
     * @param data 数据体（各 type 对应的数据对象）
     */
    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /** 获取协议类型 */
    public String getType() {
        return type;
    }

    /** 设置协议类型 */
    public void setType(String type) {
        this.type = type;
    }

    /** 获取数据体 */
    public Object getData() {
        return data;
    }

    /** 设置数据体 */
    public void setData(Object data) {
        this.data = data;
    }

    /** 获取时间戳（毫秒） */
    public long getTimestamp() {
        return timestamp;
    }

    /** 设置时间戳（通常无需手动设置） */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}