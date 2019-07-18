package com.caiya.session.test.controller;

import com.caiya.session.test.pojo.User;
import com.caiya.session.test.util.SessionConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    /**
     * 用户登录
     */
    @PostMapping(value = "/login")
    public Object login(User user, HttpServletRequest request) {
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
            HttpSession session = request.getSession();
            session.setAttribute(SessionConstant.USER, userDB);
        }

        return userDB;
    }

    /**
     * 获取用户信息
     */
    @GetMapping(value = "/info")
    public User info(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(SessionConstant.USER);
    }

    /**
     * 用户登出
     */
    @GetMapping(value = "/logout")
    public Object logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object user = session.getAttribute(SessionConstant.USER);
            if (user != null) {
                session.removeAttribute(SessionConstant.USER);
                session.invalidate();
            }
        }

        return "success";
    }

}
