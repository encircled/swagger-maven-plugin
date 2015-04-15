package com.github.kongchen.swagger.docgen.mustache;


import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kongchen.swagger.docgen.StringTypeHolder;
import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.wordnik.swagger.model.Parameter;

public class MustacheParameter {

    private final String allowableValue;

    private final String access;

    private final String defaultValue;

    private final boolean required;

    private final String description;

    private final String type;

    private String name;

    public MustacheParameter(Parameter para) {
        this.name = para.name();
        this.required = para.required();
        this.description = Utils.getStrInOption(para.description());
        this.type = para.dataType();
        this.defaultValue = Utils.getStrInOption(para.defaultValue());
        this.allowableValue = Utils.allowableValuesToString(para.allowableValues());
        this.access = Utils.getStrInOption(para.paramAccess());
    }

    String getDefaultValue() {
        return defaultValue;
    }

    public String getAllowableValue() {
        return allowableValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getAccess() {
        return access;
    }

    public String getRowType() {
        String rowType = TypeUtils.parseClassNamesFromGenericString(type).getTypeName();
        return TypeUtils.prepareClassNameForTemplate(rowType);
    }

    public List<StringTypeHolder> getGenerics() {
        return TypeUtils.parseClassNamesFromGenericString(type).getGenerics();
    }

    public boolean getHasGenerics() {
        return !TypeUtils.parseClassNamesFromGenericString(type).getGenerics().isEmpty();
    }

    @Override
    public String toString() {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }

    }
}
