package com.github.kongchen.swagger.docgen.mustache;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.model.ResponseMessage;

public class MustacheResponseMessage {

    private final int code;

    private final String message;

    private final String type;

    public MustacheResponseMessage(ResponseMessage responseMessage) {
        this.code = responseMessage.code();
        this.message = responseMessage.message();
        if (!responseMessage.responseModel().isEmpty()) {
            this.type = responseMessage.responseModel().get();
        } else {
            this.type = null;
        }
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
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
