package com.caiya.session.test.controller;

import com.caiya.session.Session;
import com.caiya.session.test.pojo.User;
import com.caiya.session.test.util.SessionConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController {


    /**
     * 用户登录
     */
    @PostMapping(value = "/login")
    public Object login(User user) {
        if (user == null || user.getLoginCode() == null || user.getPassword() == null) {
            throw new IllegalArgumentException("用户名或密码为空！");
        }

        // 根据用户名查询用户是否存在
        User userDB = new User(10001L, "菜蚜", "18612345678", "123456");// TODO mock here, userService.getUserByLoginCode(user.getLoginCode());
        if (userDB == null || !userDB.getPassword().equals(user.getPassword())) {// TODO 如果密码加密，这里需要比对加密后的字符串
            throw new IllegalArgumentException("用户名或密码错误！");
        } else {
            // 设置密码为null
            userDB.setPassword(null);
            // 保存用户会话
            super.getSession(true).setAttribute(SessionConstant.USER, userDB);
        }

        return userDB;
    }

    /**
     * 获取用户信息
     */
    @GetMapping(value = "/info")
    public User info() {
        return super.getUser();
    }

    /**
     * 用户登出
     */
    @GetMapping(value = "/logout")
    public Object logout() {
        Session session = super.getSession(false);
        if (session != null) {
            Object user = session.getAttribute(SessionConstant.USER);
            if (user != null) {
                session.removeAttribute(SessionConstant.USER);
                session.setMaxInactiveInterval(Duration.ofSeconds(0));
            }
        }

        return "success";
    }

}
