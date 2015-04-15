package com.github.kongchen.swagger.docgen.mustache;

import java.util.LinkedList;

import com.github.kongchen.swagger.docgen.TypeUtils;

public class MustacheResponseClass {

    private String className;

    private boolean isLast;

    private LinkedList<MustacheResponseClass> genericClasses = new LinkedList<>();

    private MustacheResponseClass(String responseClass, boolean isLast) {
        this.isLast = isLast;
        TypeUtils.parseClassNamesFromGenericString(responseClass);
        if (TypeUtils.isGenericString(responseClass)) {
            int genericStart = responseClass.indexOf(TypeUtils.GENERIC_START);
            int genericEnd = responseClass.lastIndexOf(TypeUtils.GENERIC_END);
            className = responseClass.substring(0, genericStart);
            // TODO
            String[] split = responseClass.substring(genericStart + 1, genericEnd).split(",");
            for (int i = 0; i < split.length; i++) {
                String generic = split[i];
                genericClasses.add(new MustacheResponseClass(generic, i == (split.length - 1)));
            }
        } else {
            this.className = responseClass;
        }
        className = TypeUtils.prepareClassNameForTemplate(className);
    }

    public LinkedList<MustacheResponseClass> getGenericClasses() {
        return genericClasses;
    }

    public void setGenericClasses(LinkedList<MustacheResponseClass> genericClasses) {
        this.genericClasses = genericClasses;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassLinkName() {
        if (genericClasses.isEmpty()) {
            return className;
        }
        return genericClasses.getFirst().getClassLinkName();
    }

    public boolean isLast() {
        return isLast;
    }

    public void setIsLast(final boolean isLast) {
        this.isLast = isLast;
    }

    // TODO delete ?
    /*public String getAllClassesAsString() {
        StringBuilder sb = new StringBuilder();
        stringifyInternal(this, sb);
        return sb.toString();
    }

    public void stringifyInternal(MustacheResponseClass responseClass, StringBuilder sb) {
        sb.append(responseClass.className);
        if (!responseClass.getGenericClasses().isEmpty()) {
            sb.append("<");
            for (MustacheResponseClass generic : responseClass.getGenericClasses()) {
                stringifyInternal(generic, sb);
            }
            sb.append(">");
        }
    }*/

}
