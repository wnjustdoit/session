package com.caiya.session.test.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = -8741794542106019386L;

    private Long id;

    private String name;

    private String loginCode;

    private String password;


    public User() {
    }

    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public User(Long id, String name, String loginCode, String password) {
        this(id, name);
        this.loginCode = loginCode;
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User)) {
            return false;
        }

        User param = (User) obj;
        return Objects.equals(id, param.id) && Objects.equals(name, param.name);
    }

}