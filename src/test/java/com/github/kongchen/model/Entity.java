package com.github.kongchen.model;

import javax.xml.bind.annotation.XmlElement;

import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Created with IntelliJ IDEA.
 * User: kongchen
 * Date: 6/4/13
 */
public interface Entity<T> {
    @XmlElement(name = "id")
    @ApiModelProperty(value = "Address' indentifier")
    Integer getId();

    void setId(Integer id);
}
