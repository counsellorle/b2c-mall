package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.dto.RegisterDTO;
import com.lxs.b2cmall.shop.entity.Employee;

import javax.servlet.http.HttpServletRequest;

public interface EmployeeService {

    Employee login(LoginDTO loginDTO, HttpServletRequest request);

    Employee register(RegisterDTO registerDTO);
}