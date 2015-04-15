package com.github.kongchen.swagger.docgen.mustache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.github.kongchen.swagger.docgen.StringTypeHolder;
import com.github.kongchen.swagger.docgen.TypeUtils;
import com.wordnik.swagger.model.Authorization;
import com.wordnik.swagger.model.Operation;
import com.wordnik.swagger.model.Parameter;
import com.wordnik.swagger.model.ResponseMessage;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

public class MustacheOperation {
    private static final Pattern genericInNotes = Pattern
            .compile("(/\\*.*<)((\\w+|((\\w+\\.)+\\w+))|(((\\w+|((\\w+\\.)+\\w+)),)+(\\w+|((\\w+\\.)+\\w+))))(>.*\\*/)");
    private final int opIndex;
    private final String httpMethod;
    private final String summary;
    private final String notes;
    private final StringTypeHolder responseClass;
    private final String nickname;
    private final List<MustacheAuthorization> authorizations = new ArrayList<MustacheAuthorization>();
    private final List<MustacheContentType> responseContentTypes = new ArrayList<MustacheContentType>();
    private final List<MustacheContentType> parameterContentTypes = new ArrayList<>();
    List<MustacheSample> samples;
    private List<StringTypeHolder> responseClasses = new ArrayList<>();
    private List<MustacheParameterSet> parameters;
    private List<MustacheResponseMessage> responseMessages = new ArrayList<MustacheResponseMessage>();

    public MustacheOperation(MustacheDocument mustacheDocument, Operation op) {
        if (op.authorizations() != null && op.authorizations().size() > 0) {
            Buffer<Authorization> authorBuffer = op.authorizations().toBuffer();
            for (Authorization authorization : JavaConversions.asJavaList(authorBuffer)) {
                this.authorizations.add(new MustacheAuthorization(authorization));
            }
        }
        this.opIndex = op.position();
        this.httpMethod = op.method();
        AbstractMap.SimpleEntry<String, String> notesAndGenericStr = parseGenericFromNotes(op.notes());
        this.notes = notesAndGenericStr.getKey();
        this.summary = op.summary();
        this.nickname = op.nickname();
        if (op.parameters() != null) {
            Buffer<Parameter> buffer = op.parameters().toBuffer();
            this.parameters = mustacheDocument.analyzeParameters(JavaConversions.asJavaList(buffer));
        }
        String responseText;
        if (op.responseClass() == null && notesAndGenericStr.getValue().isEmpty()) {
            responseText = null;
        } else {
            responseText = op.responseClass() + notesAndGenericStr.getValue();
        }
        responseClass = TypeUtils.parseClassNamesFromGenericString(responseText);
        if (op.responseMessages() != null) {
            Buffer<ResponseMessage> errorBuffer = op.responseMessages().toBuffer();
            List<ResponseMessage> responseMessages = JavaConversions.asJavaList(errorBuffer);
            for (ResponseMessage responseMessage : responseMessages) {
                if (!responseMessage.responseModel().isEmpty()) {
                    String className = responseMessage.responseModel().get();
                    this.responseClasses.add(TypeUtils.parseClassNamesFromGenericString(className));
                }
                this.responseMessages.add(new MustacheResponseMessage(responseMessage));
            }
        }
        if (parameters == null) {
            return;
        }

        List<String> produces = JavaConversions.asJavaList(op.produces());
        for (String produce : produces) {
            this.responseContentTypes.add(new MustacheContentType(produce));
        }

        List<String> consumes = JavaConversions.asJavaList(op.consumes());
        for (String consume : consumes) {
            this.parameterContentTypes.add(new MustacheContentType(consume));
        }
    }

    // TODO delete it?
    private AbstractMap.SimpleEntry<String, String> parseGenericFromNotes(String notes) {
        Scanner scanner = new Scanner(notes);
        String genericString = scanner.findInLine(genericInNotes);
        if (genericString != null) {
            return new AbstractMap.SimpleEntry<>(notes.replaceFirst(genericInNotes.pattern(), ""),
                    genericString.replaceAll("/\\*", "").replaceAll("\\*/", "").trim());
        } else {
            return new AbstractMap.SimpleEntry<>(notes, "");
        }
    }

    public List<MustacheAuthorization> getAuthorizations() {
        return authorizations;
    }

    public List<MustacheSample> getSamples() {
        return samples;
    }

    public void setSamples(List<MustacheSample> samples) {
        this.samples = samples;
    }

    public int getOpIndex() {
        return opIndex;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getSummary() {
        return summary;
    }

    public String getNotes() {
        return notes;
    }

    public String getNickname() {
        return nickname;
    }

    public List<MustacheParameterSet> getParameters() {
        return parameters;
    }

    public List<MustacheResponseMessage> getResponseMessages() {
        return responseMessages;
    }

    /**
     * @deprecated Use {@link #getResponseMessages} instead
     */
    @Deprecated
    public List<MustacheResponseMessage> getErrorResponses() {
        return responseMessages;
    }

    public StringTypeHolder getResponseClass() {
        return responseClass;
    }

    public List<StringTypeHolder> getResponseClasses() {
        return responseClasses;
    }

    public List<MustacheContentType> getResponseContentTypes() {
        return responseContentTypes;
    }

    public List<MustacheContentType> getParameterContentTypes() {
        return parameterContentTypes;
    }

}
