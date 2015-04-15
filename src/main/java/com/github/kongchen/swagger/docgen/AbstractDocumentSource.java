package com.github.kongchen.swagger.docgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSource;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSourceInfo;
import com.github.kongchen.swagger.docgen.mustache.MustacheApi;
import com.github.kongchen.swagger.docgen.mustache.OutputTemplate;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.converter.OverrideConverter;
import com.wordnik.swagger.core.util.JsonSerializer;
import com.wordnik.swagger.core.util.JsonUtil;
import com.wordnik.swagger.model.ApiListing;
import com.wordnik.swagger.model.ApiListingReference;
import com.wordnik.swagger.model.ResourceListing;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import scala.collection.Iterator;
import scala.collection.JavaConversions;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: chekong 05/13/2013
 */
public abstract class AbstractDocumentSource {

    protected final static String SWAGGER_RELATIVE_PATH = "/swagger";
    protected final static String HTML_TEMPLATE_FILE = "html.mustache";
    protected final static String MD_TEMPLATE_FILE = "markdown.mustache";
    protected final LogAdapter LOG;
//    private final String outputPath;

    protected ResourceListing serviceDocument;
    protected ObjectMapper mapper = new ObjectMapper();
    protected String overridingModels;
    List<ApiListing> validDocuments = new ArrayList<>();
    private String apiVersion;
    private ApiSourceInfo apiInfo;
    private OutputTemplate outputTemplate;
    private boolean useOutputFlatStructure;
    private Comparator<MustacheApi> apiSortComparator;
    private ApiSource apiSource;

    public AbstractDocumentSource(LogAdapter logAdapter, ApiSource apiSource) {
        LOG = logAdapter;
//        this.outputPath = apiSource.getOutputFolder();
        this.useOutputFlatStructure = apiSource.isUseOutputFlatStructure();
        this.overridingModels = apiSource.getOverridingModels();
        this.apiSource = apiSource;
    }

    public abstract void loadDocuments() throws Exception;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public OutputTemplate getOutputTemplate() {
        return outputTemplate;
    }

    public ApiSourceInfo getApiInfo() {
        return apiInfo;
    }

    public void setApiInfo(ApiSourceInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    protected void acceptDocument(ApiListing doc) {
        ApiListing newDoc = new ApiListing(doc.apiVersion(),
                doc.swaggerVersion(), doc.basePath(), doc.resourcePath(),
                doc.produces(), doc.consumes(), doc.protocols(),
                doc.authorizations(), doc.apis(), doc.models(),
                doc.description(), doc.position());
        validDocuments.add(newDoc);
    }

    public List<ApiListing> getValidDocuments() {
        return validDocuments;
    }

    public void toSwaggerDocuments(String basePath)
            throws GenerateException {
        if (basePath == null) {
            return;
        }
        String swaggerPath = apiSource.getOutputFolder() + SWAGGER_RELATIVE_PATH;
        File dir = new File(swaggerPath);
        if (dir.isFile()) {
            throw new GenerateException(String.format(
                    "Swagger-outputDirectory[%s] must be a directory!",
                    swaggerPath));
        }

        if (!dir.exists()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new GenerateException(String.format(
                        "Create Swagger-outputDirectory[%s] failed.",
                        swaggerPath));
            }
        }
        cleanupOldFiles(dir);

        prepareServiceDocument();
        // rewrite basePath in swagger-ui output file using the value in
        // configuration file.
        writeInDirectory(dir, serviceDocument, basePath);
        for (ApiListing doc : validDocuments) {
            writeInDirectory(dir, doc, basePath);
        }
    }

    public void loadOverridingModels() throws GenerateException {
        if (overridingModels != null) {
            try {
                JsonNode readTree = mapper.readTree(this.getClass()
                        .getResourceAsStream(overridingModels));
                for (JsonNode jsonNode : readTree) {
                    JsonNode classNameNode = jsonNode.get("className");
                    String className = classNameNode.asText();
                    JsonNode jsonStringNode = jsonNode.get("jsonString");
                    String jsonString = jsonStringNode.asText();
                    OverrideConverter converter = new OverrideConverter();
                    converter.add(className, jsonString);
                    ModelConverters.addConverter(converter, true);
                }
            } catch (JsonProcessingException e) {
                throw new GenerateException(
                        String.format(
                                "Swagger-overridingModels[%s] must be a valid JSON file!",
                                overridingModels), e);
            } catch (IOException e) {
                throw new GenerateException(String.format(
                        "Swagger-overridingModels[%s] not found!",
                        overridingModels), e);
            }
        }
    }

    private void cleanupOldFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith("json")) {
                    if (!f.delete()) {
                        LOG.info("Failed to delete file " + f.getPath());
                    }
                }
            }
        }
    }

    private void prepareServiceDocument() {
        List<ApiListingReference> apiListingReferences = new ArrayList<ApiListingReference>();
        for (Iterator<ApiListingReference> iterator = serviceDocument.apis()
                .iterator(); iterator.hasNext(); ) {
            ApiListingReference apiListingReference = iterator.next();
            String newPath = apiListingReference.path();
            if (useOutputFlatStructure) {
                newPath = newPath.replaceAll("/", "_");
                if (newPath.startsWith("_")) {
                    newPath = "/" + newPath.substring(1);
                }
            }
            newPath += ".{format}";
            apiListingReferences.add(new ApiListingReference(newPath,
                    apiListingReference.description(), apiListingReference
                    .position()));
        }
        // there's no setter of path for ApiListingReference, we need to create
        // a new ResourceListing for new path
        serviceDocument = new ResourceListing(serviceDocument.apiVersion(),
                serviceDocument.swaggerVersion(),
                scala.collection.immutable.List.fromIterator(JavaConversions
                        .asScalaIterator(apiListingReferences.iterator())),
                serviceDocument.authorizations(), serviceDocument.info());
    }

    protected String resourcePathToFilename(String resourcePath) {
        if (resourcePath == null) {
            return "service.json";
        }
        String name = resourcePath;
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }

        if (useOutputFlatStructure) {
            name = name.replaceAll("/", "_");
        }

        return name + ".json";
    }

    private void writeInDirectory(File dir, ApiListing apiListing,
            String basePath) throws GenerateException {
        String json = JsonSerializer.asJson(apiListing);
        writeInDirectory(dir, json, resourcePathToFilename(apiListing.resourcePath()), basePath);
    }

    private void writeInDirectory(File dir, ResourceListing resourceListing,
            String basePath) throws GenerateException {
        String json = JsonSerializer.asJson(resourceListing);
        writeInDirectory(dir, json, resourcePathToFilename(null), basePath);
    }

    private void writeInDirectory(File dir, String json, String filename, String basePath) throws GenerateException {
        OutputStream out = null;
        try {
            File serviceFile = createFile(dir, filename);
            JsonNode tree = mapper.readTree(json);
            if (basePath != null) {
                ((ObjectNode) tree).put("basePath", basePath);
            }
            out = new FileOutputStream(serviceFile);
            writeContent(out, tree);
        } catch (IOException e) {
            throw new GenerateException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Serializes json tree and writes to stream.
     *
     * @param out OutputStream of where to write output to
     * @param tree the jsonNode representation of the swagger spec
     * @throws IOException if there is a problem writing to output stream
     */
    protected void writeContent(final OutputStream out, final JsonNode tree) throws IOException {
        JsonUtil.mapper().writerWithDefaultPrettyPrinter()
                .writeValue(out, tree);
    }

    protected File createFile(File dir, String outputResourcePath)
            throws IOException {
        File serviceFile;
        int i = outputResourcePath.lastIndexOf("/");
        if (i != -1) {
            String fileName = outputResourcePath.substring(i + 1);
            String subDir = outputResourcePath.substring(0, i);
            File finalDirectory = new File(dir, subDir);
            finalDirectory.mkdirs();
            serviceFile = new File(finalDirectory, fileName);
        } else {
            serviceFile = new File(dir, outputResourcePath);
        }
        while (!serviceFile.createNewFile()) {
            serviceFile.delete();
        }
        LOG.info("Creating file " + serviceFile.getAbsolutePath());
        return serviceFile;
    }

    public OutputTemplate prepareMustacheTemplate() throws GenerateException {
        this.outputTemplate = new OutputTemplate(this);
        return outputTemplate;
    }

    public void toDocuments() throws GenerateException {
        if (outputTemplate == null) {
            prepareMustacheTemplate();
        }
        if (outputTemplate.getApiDocuments().isEmpty()) {
            LOG.warn("Nothing to write.");
            return;
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        try {
            writeToFile(resolver.getResource(HTML_TEMPLATE_FILE), apiSource.getOutputFolder() + "/api-doc.html");
            writeToFile(resolver.getResource(MD_TEMPLATE_FILE), apiSource.getOutputFolder() + "/api-doc.md");
        } catch (Exception e) {
            LOG.error("Failed to write documentation to file! " + e.getMessage());
        }
    }

    private void writeToFile(Resource templateFile, String outputPath) throws GenerateException {
        LOG.info("Writing API document to " + outputPath + ", template is " + templateFile.getFilename());

        Charset charSet = Charset.forName("UTF-8");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputPath),
                charSet);
                InputStreamReader reader = new InputStreamReader(templateFile.getInputStream(),
                        charSet)) {

            Mustache mustache = getMustacheFactory().compile(reader, templateFile.getFilename());
            mustache.execute(writer, outputTemplate)
                    .flush();
            LOG.info("Writing done.");
        } catch (Exception e) {
            throw new GenerateException(e);
        }
    }

    private DefaultMustacheFactory getMustacheFactory() {
        return new DefaultMustacheFactory(); // TODO specify folder?
    }

    public Comparator<MustacheApi> getApiSortComparator() {
        return apiSortComparator;
    }
}
