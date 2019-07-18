package com.caiya.session.test.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TestController.
 *
 * @author wangnan
 * @since 1.0
 */
@RestController
@RequestMapping()
public class TestController {


    private static final Logger logger = LoggerFactory.getLogger(TestController.class);


    @GetMapping("/test")
    public String test(HttpServletRequest request, HttpServletResponse response) {

        return "OK";
    }


}
