package com.github.kongchen.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.joda.time.DateTime;

/**
 * @author Kisel on 08.04.2015.
 */
public class User {

    private String firstName;

    private String lastName;

    private DateTime birthDate;

    private List<Pet> pets;

    @XmlElement
    private List<? extends Pet> petsWildcard;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public DateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final DateTime birthDate) {
        this.birthDate = birthDate;
    }

    public List<? extends Pet> getPetsWildcard() {
        return petsWildcard;
    }

    public void setPetsWildcard(final List<? extends Pet> petsWildcard) {
        this.petsWildcard = petsWildcard;
    }

    public List<Pet> getPets() {
        return pets;
    }

    public void setPets(final List<Pet> pets) {
        this.pets = pets;
    }
}
