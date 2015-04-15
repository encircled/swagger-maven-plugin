package com.github.kongchen.springmvc.controller;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author Kisel on 08.04.2015.
 */
public abstract class AbstractController<T extends Serializable, V extends Number> implements GeneralController<T> {

    @Override
    public List<T> test() {
        return null;
    }

    @Override
    public List<? extends T> test8() {
        return null;
    }

    @Override
    public Set<List<T>> test9() {
        return null;
    }
}
