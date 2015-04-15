package com.github.kongchen.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Created with IntelliJ IDEA.
 * User: kongchen
 * Date: 6/4/13
 */
public class Customer implements Entity<Integer> {
    private String name;

    private String surname;

    private Integer id;

    private Address address;

    private List<Email> emails;

    @ApiModelProperty(required = false, value = "For test only")
    private JAnimal animal;

    /**
     * {@inheritDoc}
     */
    @XmlElement(name = "id")
    @ApiModelProperty(required = true, value = "Customer's identifier")
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @XmlElement(name = "surname")
    @ApiModelProperty(required = true, value = "Customer's surname")
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @XmlElement(name = "name")
    @ApiModelProperty(required = true, value = "Customer's name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(required = false, name = "email")
    @ApiModelProperty(value = "The list of customer emails", required = false)
    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    @XmlElement(name = "address", required = false)
    @ApiModelProperty(required = false, value = "Customer's address")
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public JAnimal getAnimal() {
        return animal;
    }

    public void setAnimal(JAnimal animal) {
        this.animal = animal;
    }
}

