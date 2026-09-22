package com.lxs.b2cmall.shop.observer;

import com.lxs.b2cmall.shop.event.LoginEvent;
import com.lxs.b2cmall.shop.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserStatusObserver {

    @Autowired
    private EmployeeMapper employeeMapper;

    @EventListener
    public void onLoginEvent(LoginEvent event) {
        if (event.isSuccess() && event.getUserId() != null) {
            employeeMapper.updateLoginInfo(event.getUserId());
        }
    }
}