package com.github.kongchen.swagger.docgen;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.GenericTypeResolver;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: chekong
 * 05/13/2013
 */
public class TypeUtils {

    public static final Pattern genericPattern = Pattern.compile(".*<.*>.*");

    public static final Set<String> collectionClasses = new HashSet<>(Arrays.asList("Set", "set", "list", "List", "Collection", "collection"));
    public static final String GENERIC_START = "<";
    public static final String GENERIC_END = ">";

    private static final List<String> basicTypes = Arrays.asList("object", "String", "string", "boolean", "Date", "int",
            "integer", "Array", "long", "List", "void", "float", "double");

    public static String getTrueType(String dataType) {
        if (dataType == null || dataType.isEmpty()) {
            return dataType;
        }
        return parseClassNamesFromGenericString(dataType).getTypeName();
    }

    public static String filterBasicTypes(String linkType) {
        if (basicTypes.contains(linkType)) {
            return null;
        }
        return linkType;
    }

    public static String AsArrayType(String elementType) {
        return "Array[" + elementType + "]";
    }

    public static boolean isCollectionClassName(String simpleClassName) {
        return StringUtils.isNotEmpty(simpleClassName) && collectionClasses.contains(simpleClassName);
    }

    public static String prepareClassNameForTemplate(String className) {
        if (isCollectionClassName(className)) {
            return "Array";
        }
        return className;
    }

    /**
     *
     * @param declaringClass class that declares generics
     * @param genericType
     * @return
     */
    public static TypeHolder getFullTypesTree(Class<?> declaringClass, Type genericType) {
        TypeHolder root;
        if (genericType instanceof ParameterizedType) {
            root = new TypeHolder(getRowClass(genericType));
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            for (Type innerType : parameterizedType.getActualTypeArguments()) {
                if (innerType instanceof ParameterizedType) {
                    // Nested generic
                    root.generics.add(getFullTypesTree(declaringClass, innerType));
                } else if (innerType instanceof TypeVariable) {
                    root = new TypeHolder(getRowClass(genericType));
                    TypeHolder generic = new TypeHolder(resolveClassOfTypeVariable(declaringClass, (TypeVariable) innerType));
                    root.generics.add(generic);
                } else if (innerType instanceof WildcardType) {
                    WildcardType wildcardType = (WildcardType) innerType;
                    root.generics.add(getFullTypesTree(declaringClass, wildcardType.getUpperBounds()[0]));
                } else {
                    root.generics.add(new TypeHolder(getRowClass(innerType)));
                }
            }
        } else if (genericType instanceof TypeVariable) {
            root = new TypeHolder(resolveClassOfTypeVariable(declaringClass, (TypeVariable) genericType));
        } else if (genericType instanceof WildcardType) {
            // TODO impossible?
            throw new IllegalStateException();
        } else {
            root = new TypeHolder(getRowClass(genericType));
        }
        return root;
    }

    public static Class<?> getRowClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getRowClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            return getRowClass(typeVariable.getBounds()[0]);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            return getRowClass(wildcardType.getUpperBounds()[0]);
        } else {
            throw new UnsupportedOperationException(type.toString());
        }
    }

    public static Class<?> resolveClassOfTypeVariable(Class<?> targetClass, TypeVariable typeVariable) {
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
        TypeVariable<?>[] typeParameters = genericDeclaration.getTypeParameters();
        int index = -1;
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameters[i].getName().equals(typeVariable.getName())) {
                index = i;
                break;
            }
        }
        if (genericDeclaration instanceof Class) {
            return GenericTypeResolver.resolveTypeArguments(targetClass, (Class) genericDeclaration)[index];
        } else {
            Method method = (Method) genericDeclaration;
            method.getGenericReturnType();
            TypeVariable<Method> methodTypeVariable = method.getTypeParameters()[index];
            return getRowClass(methodTypeVariable.getBounds()[0]);
        }
    }

    public static boolean isGenericString(String classString) {
        return classString != null && genericPattern.matcher(classString).matches();
    }

    /**
     * For example: parse <code>[Map[String,List[Pet]]]</code> from <code>Map<String,List<Pet>></code>
     */
    public static StringTypeHolder parseClassNamesFromGenericString(String genericClass) {
        if (StringUtils.isEmpty(genericClass) || !isGenericString(genericClass)) {
            return new StringTypeHolder(genericClass);
        }

        int genericStart = genericClass.indexOf(GENERIC_START);
        String classNamePart = genericClass.substring(0, genericStart);
        String fullLeftGenericPart = genericClass.substring(genericStart + 1, genericClass.length() - 1);

        List<String> genericParts = new ArrayList<>();
        int lastCandidatePosition = 0;
        int lastSuccessCandidatePosition = 0;
        for (Integer candidatePosition : allIndexesOf(fullLeftGenericPart, ",")) {
            String candidatePart = fullLeftGenericPart.substring(lastCandidatePosition, candidatePosition);
            // Check that candidate comma is not from inner generic
            if (allIndexesOf(candidatePart, GENERIC_START).size() == allIndexesOf(candidatePart, GENERIC_END).size()) {
                genericParts.add(candidatePart);
                lastSuccessCandidatePosition = candidatePosition;
            }
            lastCandidatePosition = candidatePosition;
        }

        StringTypeHolder result = new StringTypeHolder(classNamePart);

        if (genericParts.isEmpty()) {
            result.getGenerics().add(parseClassNamesFromGenericString(fullLeftGenericPart));
        } else {
            // Add part after last comma
            genericParts.add(fullLeftGenericPart.substring(lastSuccessCandidatePosition + 1, fullLeftGenericPart.length()));
            for (String genericPart : genericParts) {
                result.getGenerics().add(parseClassNamesFromGenericString(genericPart.trim()));
            }
        }
        result.getGenerics().get(result.getGenerics().size() - 1).setIsLast(true);
        return result;
    }

    public static List<Integer> allIndexesOf(String word, String guess) {
        List<Integer> result = new ArrayList<>();
        for (int index = word.indexOf(guess); index >= 0;
                index = word.indexOf(guess, index + 1)) {
            result.add(index);
        }
        return result;
    }

    public static class TypeHolder {

        public Class<?> clazz;

        public List<TypeHolder> generics = new ArrayList<>(2);

        public TypeHolder(final Class<?> clazz) {
            this.clazz = clazz;
        }

        private static void stringifyInternal(TypeHolder typeHolder, StringBuilder sb) {
            sb.append(typeHolder.clazz.getSimpleName());
            if (!typeHolder.generics.isEmpty()) {
                sb.append(GENERIC_START);
                for (TypeHolder generic : typeHolder.generics) {
                    stringifyInternal(generic, sb);
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append(GENERIC_END);
            }
        }

        public List<Class<?>> collectAllClasses() {
            List<Class<?>> classes = new ArrayList<>();
            collectClassesInternal(this, classes);
            return classes;
        }

        public Class<?> getLastLeftLeaf() {
            if (generics.isEmpty()) {
                return clazz;
            }
            return generics.get(0).getLastLeftLeaf();
        }

        private void collectClassesInternal(TypeHolder typeHolder, List<Class<?>> classes) {
            classes.add(typeHolder.clazz);
            for (TypeHolder generic : typeHolder.generics) {
                collectClassesInternal(generic, classes);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            stringifyInternal(this, sb);
            return sb.toString();
        }

    }

}
