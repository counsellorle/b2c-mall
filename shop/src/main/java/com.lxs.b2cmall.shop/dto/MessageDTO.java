package com.lxs.b2cmall.shop.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private Integer id;
    private String title;
    private String content;
    private Integer msgType;
    private Integer isRead;
    private String createdAt;
}