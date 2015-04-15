package com.github.kongchen.swagger.docgen.spring;

import static scala.collection.immutable.List.fromIterator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSource;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.google.common.base.CharMatcher;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.converter.OverrideConverter;
import com.wordnik.swagger.core.ApiValues;
import com.wordnik.swagger.core.SwaggerSpec;
import com.wordnik.swagger.model.AllowableListValues;
import com.wordnik.swagger.model.AllowableRangeValues;
import com.wordnik.swagger.model.AllowableValues;
import com.wordnik.swagger.model.ApiDescription;
import com.wordnik.swagger.model.ApiListing;
import com.wordnik.swagger.model.Authorization;
import com.wordnik.swagger.model.AuthorizationScope;
import com.wordnik.swagger.model.Model;
import com.wordnik.swagger.model.ModelProperty;
import com.wordnik.swagger.model.ModelRef;
import com.wordnik.swagger.model.Operation;
import com.wordnik.swagger.model.Parameter;
import com.wordnik.swagger.model.ResponseMessage;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.mutable.LinkedHashMap;

/**
 * @author tedleman
 *         <p/>
 *         The use-goal of this object is to return an ApiListing object from the read() method.
 *         The listing object is populated with other api objects, contained by ApiDescriptions
 *         <p/>
 *         Generation Order:
 *         <p/>
 *         ApiListing ==> ApiDescriptions ==> Operations ==> Parameters
 *         ==> ResponseMessages
 *         <p/>
 *         Models are generated as they are detected and ModelReferences are added for each
 */
public class SpringMvcApiReader {
    private static final Option<String> DEFAULT_OPTION = Option.empty(); //<--comply with scala option to prevent nulls
    private static final String[] RESERVED_PACKAGES = { "java", "org.springframework" };
    private ApiSource apiSource;
    private ApiListing apiListing;
    private List<String> produces;
    private List<String> consumes;
    private HashMap<String, Model> models;
    private OverrideConverter overriderConverter;
    private int operationCounter = 1;


    public SpringMvcApiReader(ApiSource aSource, OverrideConverter overrideConverter) {
        apiSource = aSource;
        apiListing = null;
        models = new HashMap<>();
        produces = new ArrayList<>();
        consumes = new ArrayList<>();
        this.overriderConverter = overrideConverter;
    }

    public ApiListing read(SpringResource resource, SwaggerConfig swaggerConfig) {
        List<Method> methods = resource.getMethods();
        List<ApiDescription> apiDescriptions = new ArrayList<>();
        List<Authorization> authorizations = new ArrayList<>();
        String newBasePath = apiSource.getBasePath();
        String description = null;
        int position = 1;

        // Add the description from the controller api
        Api api = AnnotationUtils.findAnnotation(resource.getControllerClass(), Api.class);
        if (api != null) {
            description = api.description();
            position = api.position();
            if (api.authorizations() != null && api.authorizations().length > 0) {
                addAuthorization(authorizations, api.authorizations());
            }
        }

        String controllerResourcePath = resource.getControllerMapping();
//        newBasePath = generateBasePath(apiSource.getBasePath(), resourcePath);

        Map<String, Set<Method>> apiMethodMap = new HashMap<>();
        for (Method m : methods) {
            RequestMapping requestMapping = AnnotationUtils.findAnnotation(m, RequestMapping.class);
            if (requestMapping != null) {
                String path;
                if (requestMapping.value() != null && requestMapping.value().length != 0) {
                    path = generateFullPath(controllerResourcePath, requestMapping.value()[0]);
                } else {
                    path = controllerResourcePath;
                }

                Set<Method> methodsForPath = apiMethodMap.get(path);
                if (methodsForPath == null) {
                    methodsForPath = new HashSet<>();
                    apiMethodMap.put(path, methodsForPath);
                }
                methodsForPath.add(m);
            }
        }

        for (String p : apiMethodMap.keySet()) {
            List<Operation> operations = new ArrayList<>();
            for (Method m : apiMethodMap.get(p)) {
                operations.add(generateOperation(m, resource.getControllerClass())); // TODO
            }
            //reorder operations
            Collections.sort(operations, new Comparator<Operation>() {
                @Override
                public int compare(Operation o1, Operation o2) {
                    return o1.position() - o2.position();
                }
            });

            apiDescriptions.add(new ApiDescription(p, DEFAULT_OPTION,
                    fromIterator(JavaConversions.asScalaIterator(operations.iterator())), false));
        }

        apiListing = new ApiListing(swaggerConfig.apiVersion(), swaggerConfig.getSwaggerVersion(), newBasePath, controllerResourcePath,
                fromIterator(JavaConversions.asScalaIterator(produces.iterator())),
                fromIterator(JavaConversions.asScalaIterator(consumes.iterator())),
                fromIterator(JavaConversions.asScalaIterator(Collections.<String>emptyList().iterator())),
                fromIterator(JavaConversions.asScalaIterator(authorizations.iterator())),
                fromIterator(JavaConversions.asScalaIterator(apiDescriptions.iterator())),
                generateModels(models), Option.apply(description), position);
        return apiListing;
    }

    private void addAuthorization(List<Authorization> authorizations, com.wordnik.swagger.annotations.Authorization[] annotations) {
        for (com.wordnik.swagger.annotations.Authorization authorization : annotations) {
            List<AuthorizationScope> scopes = new ArrayList<>();
            for (com.wordnik.swagger.annotations.AuthorizationScope scope : authorization.scopes()) {
                scopes.add(new AuthorizationScope(scope.scope(), scope.description()));
            }
            authorizations.add(new Authorization(authorization.value(), scopes.toArray(new AuthorizationScope[scopes.size()])));
        }
    }

    //--------Swagger Resource Generators--------//

    @SuppressWarnings("unused")
    private String generateBasePath(String bPath, String rPath) {
        String domain = "";

        //check for first & trailing backslash
        if (bPath.lastIndexOf('/') != (bPath.length() - 1) && StringUtils.isNotEmpty(domain)) {
            bPath = bPath + '/';
        }

        //TODO this should be done elsewhere
//        if (this.resourcePath.charAt(0) != '/') {
//            this.resourcePath = '/' + this.resourcePath;
//        }

        return bPath + domain;
    }

    private String generateFullPath(String basePath, String methodPath) {
        if (StringUtils.isNotEmpty(methodPath)) {
            return basePath + (methodPath.startsWith("/") ? methodPath : '/' + methodPath);
        } else {
            return basePath;
        }
    }

    /**
     * Generates operations for the ApiDescription
     *
     * @return Operation
     */
    private Operation generateOperation(Method m, Class<?> controllerClass) {
        ApiOperation apiOperation = AnnotationUtils.findAnnotation(m, ApiOperation.class);
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(m, RequestMapping.class);
//        ResponseBody responseBody = AnnotationUtils.findAnnotation(m, ResponseBody.class); // TODO ok?
        String responseBodyName = "";
        String method = null;
        String description = m.getName();
        String notes = "";
        List<String> opProduces = new ArrayList<>();
        List<String> opConsumes = new ArrayList<>();
        List<Parameter> parameters = new ArrayList<>();
        List<ResponseMessage> responseMessages;
        List<Authorization> authorizations = new ArrayList<>();

        if (requestMapping.produces() != null) {
            opProduces = Arrays.asList(requestMapping.produces());
            for (String str : opProduces) {
                if (!produces.contains(str)) {
                    produces.add(str);
                }
            }
        }
        if (requestMapping.consumes() != null) {
            opConsumes = Arrays.asList(requestMapping.consumes());
            for (String str : opConsumes) {
                if (!consumes.contains(str)) {
                    consumes.add(str);
                }
            }
        }

        if (apiOperation != null) {
            description = apiOperation.value();
            notes = apiOperation.notes();
            if (apiOperation.authorizations() != null && apiOperation.authorizations().length > 0) {
                addAuthorization(authorizations, apiOperation.authorizations());
            }
        }

        // Response HTTP status and reason
        responseMessages = generateResponseMessages(m);

        TypeUtils.TypeHolder returnTypesTree = TypeUtils.getFullTypesTree(controllerClass, m.getGenericReturnType());
        responseBodyName = returnTypesTree.toString();
        if (responseBodyName.equals("void")) {
            responseBodyName = null;
        } else {
            for (Class<?> modelCandidateClass : returnTypesTree.collectAllClasses()) {
                addToModels(modelCandidateClass);
            }
        }

        if (requestMapping.method() != null && requestMapping.method().length != 0) {
            method = requestMapping.method()[0].toString();
        } else {
            method = "GET";
        }

        if (m.getParameterTypes() != null) {
            parameters = generateParameters(controllerClass, m);
        }

        return new Operation(method,
                description, notes, responseBodyName, description, operationCounter++,
                fromIterator(JavaConversions.asScalaIterator(opProduces.iterator())),
                fromIterator(JavaConversions.asScalaIterator(opConsumes.iterator())),
                null,
                fromIterator(JavaConversions.asScalaIterator(authorizations.iterator())),
                fromIterator(JavaConversions.asScalaIterator(parameters.iterator())),
                fromIterator(JavaConversions.asScalaIterator(responseMessages.iterator())),
                DEFAULT_OPTION);
    }

    /**
     * Generates parameters for each Operation
     *
     * @return List<Parameter>
     */
    private List<Parameter> generateParameters(Class<?> controllerClass, Method m) {
        Annotation[][] annotations = m.getParameterAnnotations();
        List<Parameter> params = new ArrayList<>();
        for (int i = 0; i < annotations.length; i++) { //loops through parameters
            AllowableValues allowed = null;
            String dataType;
            String type = "";
            String name = "";
            boolean required = true;
            Annotation[] anns = annotations[i];
            String description = "";
            List<String> allowableValuesList;

            for (Annotation annotation : anns) {
                if (annotation.annotationType().equals(PathVariable.class)) {
                    PathVariable pathVariable = (PathVariable) annotation;
                    name = pathVariable.value();
                    type = ApiValues.TYPE_PATH();
                } else if (annotation.annotationType().equals(RequestBody.class)) {
                    RequestBody requestBody = (RequestBody) annotation;
                    type = ApiValues.TYPE_BODY();
                    required = requestBody.required();
                } else if (annotation.annotationType().equals(RequestParam.class)) {
                    RequestParam requestParam = (RequestParam) annotation;
                    name = requestParam.value();
                    type = ApiValues.TYPE_QUERY();
                    required = requestParam.required();
                } else if (annotation.annotationType().equals(RequestHeader.class)) {
                    RequestHeader requestHeader = (RequestHeader) annotation;
                    name = requestHeader.value();
                    type = ApiValues.TYPE_HEADER();
                    required = requestHeader.required();
                } else if (annotation.annotationType().equals(ApiParam.class)) {
                    try {
                        ApiParam apiParam = (ApiParam) annotation;
                        if (apiParam.value() != null)
                            description = apiParam.value();
                        if (apiParam.allowableValues() != null) {
                            if (apiParam.allowableValues().startsWith("range")) {
                                String range = apiParam.allowableValues().substring("range".length());
                                String min, max;
                                Pattern pattern = Pattern.compile("\\[(.+),(.+)\\]");
                                Matcher matcher = pattern.matcher(range);
                                if (matcher.matches()) {
                                    min = matcher.group(1);
                                    max = matcher.group(2);
                                    allowed = new AllowableRangeValues(min, max);
                                }
                            } else {
                                String allowableValues = CharMatcher.anyOf("[] ").removeFrom(apiParam.allowableValues());
                                if (!(allowableValues.equals(""))) {
                                    allowableValuesList = Arrays.asList(allowableValues.split(","));
                                    allowed = new AllowableListValues(
                                            fromIterator(JavaConversions.asScalaIterator(allowableValuesList.iterator())),
                                            "LIST");
                                }
                            }


                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Type paramGenericType = m.getGenericParameterTypes()[i];
            TypeUtils.TypeHolder fullParamTypesTree = TypeUtils.getFullTypesTree(controllerClass, paramGenericType);

            dataType = fullParamTypesTree.toString();
            for (Class<?> modelCandidateClass : fullParamTypesTree.collectAllClasses()) {

                addToModels(modelCandidateClass);
            }

            params.add(new Parameter(name, Option.apply(description), DEFAULT_OPTION, required, false, dataType,
                    allowed, type, DEFAULT_OPTION));
        }
        return params;
    }


    /**
     * Generates response messages for each Operation
     *
     */
    private List<ResponseMessage> generateResponseMessages(Method m) {
        List<ResponseMessage> responseMessages = new ArrayList<>();
        ApiResponses apiresponses = AnnotationUtils.findAnnotation(m, ApiResponses.class);
        if (apiresponses != null) {
            for (ApiResponse apiResponse : apiresponses.value()) {
                if (apiResponse.response() == null || apiResponse.response().equals(java.lang.Void.class)) {
                    responseMessages.add(new ResponseMessage(apiResponse.code(), apiResponse.message(), Option.<String>empty()));
                } else {
                    addToModels(apiResponse.response());
                    responseMessages.add(new ResponseMessage(apiResponse.code(), apiResponse.message(), Option.apply(apiResponse.response().getSimpleName())));
                }
            }
        } else {
            ResponseStatus responseStatus = AnnotationUtils.findAnnotation(m, ResponseStatus.class);
            if (responseStatus != null) {
                String reason = responseStatus.reason();
                if (reason == null || reason.isEmpty()) {
                    reason = responseStatus.value().name();
                }
                responseMessages.add(new ResponseMessage(responseStatus.value().value(), reason, DEFAULT_OPTION));
            }
        }
        return responseMessages;
    }

    /**
     * Generates a Model object for a Java class. Takes properties from either fields or methods.
     * Recursion occurs if a ModelProperty needs to be modeled
     *
     * @return Model
     */
    private Model generateModel(Class<?> clazz) {
        ApiModel apiModel = AnnotationUtils.findAnnotation(clazz, ApiModel.class);
        ModelRef modelRef = null;
        String modelDescription = "";
        LinkedHashMap<String, ModelProperty> modelProps = new LinkedHashMap<>();
        List<String> subTypes = new ArrayList<>();

        if (apiModel != null) {
            modelDescription = apiModel.description();
        }

        //<--Model properties from fields-->
        int position = 1;
        for (Field field : clazz.getDeclaredFields()) {
            //Only use fields if they are annotated - otherwise use methods
            XmlElement xmlElement;
            ApiModelProperty amp;
            Class<?> c;
            String name;
            boolean required = false;
            String description = "";
            if (!(field.getType().equals(clazz))) {
                //if the types are the same, model will already be generated
                modelRef = generateModelRef(clazz, field); //recursive IFF there is a generic sub-type to be modeled
            }
            if (field.isAnnotationPresent(XmlElement.class) ||
                    field.isAnnotationPresent(ApiModelProperty.class)) {
                if (field.getAnnotation(XmlElement.class) != null) {
                    xmlElement = field.getAnnotation(XmlElement.class);
                    required = xmlElement.required();
                }
                if (field.getAnnotation(ApiModelProperty.class) != null) {
                    amp = field.getAnnotation(ApiModelProperty.class);
                    if (!required) { //if required has already been changed to true, it was noted in XmlElement
                        required = amp.required();
                    }
                    description = amp.value();
                }

                if (!(field.getType().equals(clazz))) {
                    c = field.getType();
                    name = field.getName();
                    addToModels(c);
                    subTypes.add(c.getSimpleName());
                    modelProps.put(name, generateModelProperty(c, position++, required, modelRef, description));
                }
            }
        }

        //<--Model properties from methods-->
        int i = 0;
        for (Method m : clazz.getMethods()) {
            boolean required = false;
            String description = "";
            ApiModelProperty amp;
            //look for required field in XmlElement annotation

            if (!(m.getReturnType().equals(clazz))) {
                modelRef = generateModelRef(clazz, m); //recursive IFF there is a generic sub-type to be modeled
            }

            if (m.isAnnotationPresent(XmlElement.class)) {
                XmlElement xmlElement = m.getAnnotation(XmlElement.class);
                required = xmlElement.required();
            } else if (m.isAnnotationPresent(JsonIgnore.class)
                    || m.isAnnotationPresent(XmlTransient.class)) {
                continue; //ignored fields
            }

            if (m.getAnnotation(ApiModelProperty.class) != null) {
                amp = m.getAnnotation(ApiModelProperty.class);
                required = amp.required();
                description = amp.value();
            }

            //get model properties from methods
            if ((m.getName().startsWith("get") || m.getName().startsWith("is"))
                    && !(m.getName().equals("getClass"))) {
                Class<?> c = m.getReturnType();
                String name = null;
                try {
                    if (m.getName().startsWith("get")) {
                        name = m.getName().substring(3);
                    } else {
                        name = m.getName().substring(2);
                    }
                    name = org.apache.commons.lang3.StringUtils.uncapitalize(name);
                } catch (Exception e) {
                    // ignored
                }
                if (!(m.getReturnType().equals(clazz))) {
                    addToModels(c); //recursive
                }
                if (StringUtils.isNotEmpty(name) && !modelProps.contains(name)) {
                    modelProps.put(name, generateModelProperty(c, i, required, modelRef, description));
                }

            }
            i++;
        }

        return new Model(clazz.getSimpleName(), clazz.getSimpleName(), clazz.getCanonicalName(), modelProps,
                Option.apply(modelDescription), DEFAULT_OPTION, DEFAULT_OPTION,
                fromIterator(JavaConversions.asScalaIterator(subTypes.iterator())));
    }

    /**
     * Generates a ModelProperty for given model class. Supports String enumerations only.
     *
     * @return ModelProperty
     */
    private ModelProperty generateModelProperty(Class<?> clazz, int position, boolean required, ModelRef modelRef,
            String description) {
        AllowableListValues allowed = null;
        String name = clazz.getSimpleName();

        if (!(isModel(clazz))) {
            name = name.toLowerCase();
        }
        //check for enumerated values - currently strings only
        //TODO: support ranges
        if (clazz.isEnum()) {
            List<String> enums = new ArrayList<>();
            for (Object obj : clazz.getEnumConstants()) {
                enums.add(obj.toString());
            }
            allowed = new AllowableListValues(
                    fromIterator(JavaConversions.asScalaIterator(enums.iterator())), "LIST");
        }

        return new ModelProperty(name,
                clazz.getCanonicalName(), position, required, Option.apply(description), allowed,
                Option.apply(modelRef));
    }

    /**
     * Generates a model reference based on a method
     *
     * @return ModelRef
     */
    private ModelRef generateModelRef(Class<?> clazz, Method m) {
        ModelRef modelRef = null; //can be null
        if (Collection.class.isAssignableFrom(m.getReturnType()) || m.getReturnType().equals(ResponseEntity.class)
                || m.getReturnType().equals(JAXBElement.class)) {
            TypeUtils.TypeHolder fullTypesTree = TypeUtils.getFullTypesTree(clazz, m.getGenericReturnType());
            Class<?> c = fullTypesTree.getLastLeftLeaf();
            if (isModel(c) && !(c.equals(clazz))) {
                addToModels(c);
                modelRef = new ModelRef(c.getSimpleName(), Option.apply(c.getSimpleName()),
                        Option.apply(c.getSimpleName()));
            } else {
                modelRef = new ModelRef(c.getSimpleName().toLowerCase(), DEFAULT_OPTION, DEFAULT_OPTION);
            }
        }
        return modelRef;
    }

    /**
     * Generates a model reference based on a field
     *
     * @return ModelRef
     */
    private ModelRef generateModelRef(Class<?> clazz, Field f) {
        // TODO not nice in template
        ModelRef modelRef = null;
        if (Collection.class.isAssignableFrom(f.getType()) || f.getType().equals(ResponseEntity.class)
                || f.getType().equals(JAXBElement.class)) {
            TypeUtils.TypeHolder fullTypesTree = TypeUtils.getFullTypesTree(clazz, f.getGenericType());
            Class<?> c = fullTypesTree.getLastLeftLeaf();
            if (isModel(c) && !(c.equals(clazz))) {
                addToModels(c);
                modelRef = new ModelRef(c.getSimpleName(), Option.apply(c.getSimpleName()),
                        Option.apply(c.getSimpleName()));
            } else {
                modelRef = new ModelRef(c.getSimpleName().toLowerCase(), DEFAULT_OPTION, DEFAULT_OPTION);
            }
        }
        if (Map.class.isAssignableFrom(f.getType())) {
            TypeUtils.TypeHolder fullTypesTree = TypeUtils.getFullTypesTree(clazz, f.getGenericType());
            String s = fullTypesTree.generics.get(0).toString() + ", " + fullTypesTree.generics.get(1).toString();
            modelRef = new ModelRef(s, DEFAULT_OPTION, DEFAULT_OPTION);
        }
        return modelRef;
    }

    //-------------Helper Methods------------//

    private Class<?> getGenericSubtype(Class<?> clazz, Type t) {
        if (!(clazz.getName().equals("void") || t.toString().equals("void"))) {
            try {
                ParameterizedType paramType = (ParameterizedType) t;
                Type[] argTypes = paramType.getActualTypeArguments();
                if (argTypes.length > 0) {
                    return (Class<?>) argTypes[0];
                }
            } catch (ClassCastException e) {
                //FIXME: find out why this happens to only certain types
            }
        }
        return clazz;
    }

    private void addToModels(Class<?> clazz) {
        if (isModel(clazz) && !(models.containsKey(clazz.getSimpleName()))) {
            //put the key first in models to avoid stackoverflow
            models.put(clazz.getSimpleName(), new Model(null, null, null, null, null, null, null, null));
            scala.collection.mutable.HashMap<String, Option<Model>> overriderMap = this.overriderConverter.overrides();
            if (overriderMap.contains(clazz.getCanonicalName())) {
                Option<Option<Model>> m = overriderMap.get(clazz.getCanonicalName());
                models.put(clazz.getSimpleName(), m.get().get());
            } else {
                models.put(clazz.getSimpleName(), generateModel(clazz));
            }
        }
    }

    private boolean isModel(Class<?> clazz) {
        try {
            for (String str : RESERVED_PACKAGES) {
                if (clazz.getPackage().getName().contains(str)) {
                    return false;
                }
            }
            return !SwaggerSpec.baseTypes().contains(clazz.getSimpleName().toLowerCase());
        } catch (NullPointerException e) { //null pointer for package names - wouldn't model something without a package. skip
            return false;
        }
    }

    private String generateTypeString(String clazzName) {
        String typeString = clazzName;
        if (SwaggerSpec.baseTypes().contains(clazzName.toLowerCase())) {
            typeString = clazzName.toLowerCase();
        }
        return typeString;
    }

    private Option<scala.collection.immutable.Map<String, Model>> generateModels(HashMap<String, Model> javaModelMap) {
        return Option.apply(Utils.toScalaImmutableMap(javaModelMap));
    }


}
