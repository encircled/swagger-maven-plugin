package com.github.kongchen.model;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import org.joda.time.DateTime;

/**
 * @author Kisel on 07.04.2015.
 */
public class Pet implements Serializable {

    private String nickName;

    private Integer age;

    private User user;

    @XmlElement
    private Map<User, DateTime> timeMap;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(final String nickName) {
        this.nickName = nickName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(final Integer age) {
        this.age = age;
    }

    public Map<User, DateTime> getTimeMap() {
        return timeMap;
    }

    public void setTimeMap(final Map<User, DateTime> timeMap) {
        this.timeMap = timeMap;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }
}

