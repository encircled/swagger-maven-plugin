package com.github.kongchen.model;

/**
 * @author Kisel on 13.04.2015.
 */
public class Request<T> {

    private T content;

    public T getContent() {
        return content;
    }

    public void setContent(final T content) {
        this.content = content;
    }

}
