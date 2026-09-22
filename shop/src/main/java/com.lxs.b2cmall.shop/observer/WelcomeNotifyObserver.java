package com.lxs.b2cmall.shop.observer;

import com.lxs.b2cmall.shop.entity.Message;
import com.lxs.b2cmall.shop.event.RegisterEvent;
import com.lxs.b2cmall.shop.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class WelcomeNotifyObserver {

    @Autowired
    private MessageMapper messageMapper;

    @EventListener
    public void onRegisterEvent(RegisterEvent event) {
        Message message = new Message();
        message.setShopId(event.getShopId());
        message.setSenderId(event.getEmployeeId());
        message.setTitle("欢迎加入");
        message.setContent("尊敬的 " + event.getShopName() + " 管理员 " +
                event.getUsername() + "，欢迎使用 B2C 商城系统！");
        message.setMsgType(1);
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        messageMapper.insert(message);
    }
}