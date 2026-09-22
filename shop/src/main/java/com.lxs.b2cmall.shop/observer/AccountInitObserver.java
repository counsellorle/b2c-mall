package com.lxs.b2cmall.shop.observer;

import com.lxs.b2cmall.shop.event.RegisterEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AccountInitObserver {

    @EventListener
    public void onRegisterEvent(RegisterEvent event) {
        System.out.println("账号初始化完成: userId=" + event.getEmployeeId());
    }
}