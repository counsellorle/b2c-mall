package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.dto.RegisterDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.entity.Shop;
import com.lxs.b2cmall.shop.event.LoginEvent;
import com.lxs.b2cmall.shop.event.RegisterEvent;
import com.lxs.b2cmall.shop.mapper.EmployeeMapper;
import com.lxs.b2cmall.shop.mapper.ShopMapper;
import com.lxs.b2cmall.shop.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Employee login(LoginDTO loginDTO, HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");

        Employee employee = employeeMapper.findByUsername(loginDTO.getUsername());
        if (employee == null) {
            eventPublisher.publishEvent(new LoginEvent(null, ip, ua, ua, false));
            throw new RuntimeException("账号不存在");
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), employee.getPassword())) {
            eventPublisher.publishEvent(new LoginEvent(employee.getId(), ip, ua, ua, false));
            throw new RuntimeException("密码错误");
        }

        if (employee.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        eventPublisher.publishEvent(new LoginEvent(employee.getId(), ip, ua, ua, true));

        employee.setPassword(null);
        return employee;
    }

    @Override
    public Employee register(RegisterDTO registerDTO) {
        Employee existing = employeeMapper.findByUsername(registerDTO.getUsername());
        if (existing != null) {
            throw new RuntimeException("账号已存在");
        }

        String hashedPw = BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt());
        String now = LocalDateTime.now().format(FMT);

        Shop shop = new Shop();
        shop.setShopName(registerDTO.getShopName());
        shop.setAdminAccount(registerDTO.getUsername());
        shop.setAdminPassword(hashedPw);
        shop.setLogoUrl("/logos/default.png");
        shop.setStatus(1);
        shop.setCreatedAt(now);
        shop.setUpdatedAt(now);
        shopMapper.insert(shop);

        Employee employee = new Employee();
        employee.setShopId(shop.getId());
        employee.setUsername(registerDTO.getUsername());
        employee.setPassword(hashedPw);
        employee.setAvatarUrl("/avatars/default.png");
        employee.setLoginCount(0);
        employee.setStatus(1);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        employeeMapper.insert(employee);

        eventPublisher.publishEvent(new RegisterEvent(shop.getId(), employee.getId(),
                shop.getShopName(), employee.getUsername()));

        employee.setPassword(null);
        return employee;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}