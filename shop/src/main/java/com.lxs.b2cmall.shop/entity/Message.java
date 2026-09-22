package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Message {
    private Integer id;
    private Integer shopId;
    private Integer senderId;
    private String title;
    private String content;
    private Integer msgType;
    private Integer isRead;
    private String readTime;
    private String createdAt;
}