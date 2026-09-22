package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.dto.RegisterDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.service.EmployeeService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/employee")
public class LoginController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    public Result<Employee> login(@Valid @RequestBody LoginDTO loginDTO,
                                  HttpServletRequest request) {
        try {
            Employee employee = employeeService.login(loginDTO, request);
            return Result.success(employee);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Employee> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            Employee employee = employeeService.register(registerDTO);
            return Result.success(employee);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}