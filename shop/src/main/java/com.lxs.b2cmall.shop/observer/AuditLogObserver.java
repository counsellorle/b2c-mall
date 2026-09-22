package com.lxs.b2cmall.shop.observer;

import com.lxs.b2cmall.shop.entity.LoginLog;
import com.lxs.b2cmall.shop.event.LoginEvent;
import com.lxs.b2cmall.shop.mapper.LoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuditLogObserver {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @EventListener
    public void onLoginEvent(LoginEvent event) {
        LoginLog log = new LoginLog();
        log.setUserId(event.getUserId());
        log.setLoginIp(event.getLoginIp());
        log.setLoginDevice(event.getLoginDevice());
        log.setLoginLocation("待解析");
        log.setLoginTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.setUserAgent(event.getUserAgent());
        log.setStatus(event.isSuccess() ? 1 : 0);
        loginLogMapper.insert(log);
    }
}