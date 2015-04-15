package com.github.kongchen.swagger.docgen.mustache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kongchen.swagger.docgen.DocTemplateConstants;
import com.github.kongchen.swagger.docgen.StringTypeHolder;
import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.wordnik.swagger.core.ApiValues;
import com.wordnik.swagger.core.util.ModelUtil;
import com.wordnik.swagger.model.ApiListing;
import com.wordnik.swagger.model.Model;
import com.wordnik.swagger.model.ModelProperty;
import com.wordnik.swagger.model.ModelRef;
import com.wordnik.swagger.model.Parameter;
import scala.Option;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.mutable.LinkedEntry;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: chekong
 */
public class MustacheDocument implements Comparable<MustacheDocument> {
    protected static final String VOID = "void";

    protected static final String ARRAY = "Array";

    private static final String LIST = "List";
    @JsonIgnore
    private static final Map<String, Integer> hashValueMap = new HashMap<>();

    static {
        hashValueMap.put(ApiValues.TYPE_HEADER(), 1);
        hashValueMap.put(ApiValues.TYPE_PATH(), 2);
        hashValueMap.put(ApiValues.TYPE_QUERY(), 3);
        hashValueMap.put(ApiValues.TYPE_BODY(), 4);
        hashValueMap.put(ApiValues.TYPE_FORM(), 5);
        hashValueMap.put(ApiValues.TYPE_COOKIE(), 6);
        hashValueMap.put(ApiValues.TYPE_MATRIX(), 7);
        hashValueMap.put(DocTemplateConstants.TYPE_RESPONSE_HEADER, 8);
    }

    @JsonIgnore
    private final Map<String, Model> models = new HashMap<>();
    private int index;
    private String resourcePath;
    private String description;
    private List<MustacheApi> apis = new ArrayList<>();
    private List<MustacheContentType> responseContentTypes = new ArrayList<>();
    private List<MustacheContentType> parameterContentTypes = new ArrayList<>();
    @JsonIgnore
    private Set<String> requestTypes = new LinkedHashSet<>();
    @JsonIgnore
    private Set<String> responseTypes = new LinkedHashSet<>();

    public MustacheDocument(ApiListing apiListing) {
        if (!apiListing.models().isEmpty()) {
            models.putAll(JavaConversions.mapAsJavaMap(apiListing.models().get()));
        }
        this.resourcePath = apiListing.resourcePath();
        this.index = apiListing.position();
        this.apis = new ArrayList<>(apiListing.apis().size());
        this.description = Utils.getStrInOption(apiListing.description());
        this.responseContentTypes = new ArrayList<>(apiListing.produces().size());
        this.parameterContentTypes = new ArrayList<>(apiListing.consumes().size());
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MustacheApi> getApis() {
        return apis;
    }

    public void setApis(List<MustacheApi> apis) {
        this.apis = apis;
    }

    public Set<String> getRequestTypes() {
        return requestTypes;
    }

    public Set<String> getResponseTypes() {
        return responseTypes;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<MustacheContentType> getResponseContentTypes() {
        return responseContentTypes;
    }

    public List<MustacheContentType> getParameterContentTypes() {
        return parameterContentTypes;
    }

    public void addResponseType(StringTypeHolder clz) {
        if (clz.getTypeName() == null) {
            return;
        }
        String newName = addModels(JavaConversions.mapAsJavaMap(ModelUtil.modelAndDependencies(clz.getTypeName())));
        if (newName == null) {
            responseTypes.add(clz.getTypeName());
            return;
        }
        if (newName.equals(clz.getTypeName())) {
            responseTypes.add(newName);
        }
    }

    public void addResponseContentTypes(MustacheContentType responseContentTypes) {
        this.responseContentTypes.add(responseContentTypes);
    }

    public void addParameterContentTypes(MustacheContentType parameterContentTypes) {
        this.parameterContentTypes.add(parameterContentTypes);
    }

    public List<MustacheParameterSet> analyzeParameters(List<Parameter> parameters) {
        if (parameters == null) return null;
        List<MustacheParameterSet> parameterList = new ArrayList<>();

        Map<String, List<MustacheParameter>> paraMap = toParameterTypeMap(parameters);

        for (Map.Entry<String, List<MustacheParameter>> entry : paraMap.entrySet()) {
            parameterList.add(new MustacheParameterSet(entry));
        }

        // make sure parameter order is 1.header 2.path 3.query 4.body 5.response header
        Collections.sort(parameterList, new Comparator<MustacheParameterSet>() {

            @Override
            public int compare(MustacheParameterSet o1, MustacheParameterSet o2) {
                return hashValue(o1) - hashValue(o2);
            }

            private int hashValue(MustacheParameterSet parameterSet) {
                if (parameterSet == null || parameterSet.getParamType() == null || parameterSet.getParamType().trim().length() == 0) {
                    return 0;
                } else {
                    return hashValueMap.get(parameterSet.getParamType());
                }
            }
        });
        return parameterList;
    }

    private Map<String, List<MustacheParameter>> toParameterTypeMap(List<Parameter> parameters) {
        Map<String, List<MustacheParameter>> paraMap = new HashMap<>();

        for (Parameter para : parameters) {
            MustacheParameter mustacheParameter = analyzeParameter(para);

            List<MustacheParameter> paraList = paraMap.get(para.paramType());
            if (paraList == null) {
                paraList = new LinkedList<>();
                paraMap.put(para.paramType(), paraList);
            }

            paraList.add(mustacheParameter);
        }
        return paraMap;
    }

    private MustacheParameter analyzeParameter(Parameter para) {
        MustacheParameter mustacheParameter = new MustacheParameter(para);
        if (para.dataType() != null) {
            List<String> parsedClasses = TypeUtils.parseClassNamesFromGenericString(para.dataType()).collectAllTypes();
            for (String clazzName : parsedClasses) {
                requestTypes.add(clazzName);
            }
        }

        if (para.name() != null) {
            mustacheParameter.setName(para.name());
        } else {
            mustacheParameter.setName(para.dataType());
        }

        return mustacheParameter;
    }

    public List<MustacheItem> analyzeDataTypes(String responseClass) {
        List<MustacheItem> mustacheItemList = new ArrayList<>();
        if (responseClass == null || responseClass.equals(VOID)) {
            return mustacheItemList;
        }

        Model field = models.get(responseClass);
        if (field != null && field.properties() != null) {

            for (Iterator<LinkedEntry<String, ModelProperty>> it = field.properties().entriesIterator(); it.hasNext(); ) {
                LinkedEntry<String, ModelProperty> entry = it.next();
                MustacheItem mustacheItem = new MustacheItem(entry.key(), entry.value());

                Option<ModelRef> itemOption = entry.value().items();
                ModelRef item = itemOption.isEmpty() ? null : itemOption.get();

                if (mustacheItem.getType().equalsIgnoreCase(ARRAY)
                        || mustacheItem.getType().equalsIgnoreCase(LIST)) {
                    handleArrayType(mustacheItem, item);
                } else if (mustacheItem.getType().equalsIgnoreCase("map")) {
                    String mapGenerics = item != null ? item.type() : "";
                    mustacheItem.setType("Map[" + mapGenerics + "]");
                } else if (models.get(mustacheItem.getType()) != null) {
                    responseTypes.add(mustacheItem.getType());
                }

                mustacheItemList.add(mustacheItem);
            }
        }
        Collections.sort(mustacheItemList, new Comparator<MustacheItem>() {
            @Override
            public int compare(MustacheItem o1, MustacheItem o2) {
                if (o1 != null && o2 != null) {
                    return o1.getPosition() - o2.getPosition();
                } else {
                    return 0;
                }
            }
        });
        return mustacheItemList;
    }

    private void handleArrayType(MustacheItem mustacheItem, ModelRef item) {
        if (item != null) {
            if (item.type() == null && item.ref() != null) {
                mustacheItem.setTypeAsArray(Utils.getStrInOption(item.ref()));
                responseTypes.add(Utils.getStrInOption(item.ref()));
            } else {
                mustacheItem.setTypeAsArray(item.type());
            }
        }
    }

    @Override
    public int compareTo(MustacheDocument o) {
        if (o == null) {
            return 1;
        }
        return this.getResourcePath().compareTo(o.getResourcePath());
    }

    public String addModels(Map<String, Model> modelMap) {
        if (modelMap == null || modelMap.isEmpty()) {
            return null;
        }
        for (String key : modelMap.keySet()) {
            models.put(key, modelMap.get(key));
        }
        return modelMap.keySet().iterator().next();
    }
}

