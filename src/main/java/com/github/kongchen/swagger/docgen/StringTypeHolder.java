package com.github.kongchen.swagger.docgen;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kisel on 15.04.2015.
 */
public class StringTypeHolder {

    private String typeName;

    private List<StringTypeHolder> generics = new ArrayList<>(2);

    // For templates
    private boolean isLast = false;

    public StringTypeHolder(final String typeName) {
        this.typeName = typeName;
    }

    public List<String> collectAllTypes() {
        List<String> classes = new ArrayList<>();
        collectTypesInternal(this, classes);
        return classes;
    }

    private void collectTypesInternal(StringTypeHolder typeHolder, List<String> classes) {
        classes.add(typeHolder.typeName);
        for (StringTypeHolder generic : typeHolder.generics) {
            collectTypesInternal(generic, classes);
        }
    }

    public boolean getHasGenerics() {
        return !generics.isEmpty();
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public List<StringTypeHolder> getGenerics() {
        return generics;
    }

    public void setGenerics(final List<StringTypeHolder> generics) {
        this.generics = generics;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setIsLast(final boolean isLast) {
        this.isLast = isLast;
    }

}
