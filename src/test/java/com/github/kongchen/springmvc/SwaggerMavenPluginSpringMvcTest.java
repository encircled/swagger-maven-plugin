package com.github.kongchen.springmvc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kongchen.springmvc.controller.PetController;
import com.github.kongchen.swagger.docgen.StringTypeHolder;
import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiDocumentMojo;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author chekong
 */

public class SwaggerMavenPluginSpringMvcTest extends AbstractMojoTestCase {

    private final File swaggerOutputDir = new File(getBasedir(), "generated/swagger-ui-springmvc");
    private final File docOutput = new File(getBasedir(), "generated/document-springmvc.html");
    private ApiDocumentMojo mojo;

    @BeforeMethod
    protected void setUp() throws Exception {
        super.setUp();
        try {
            FileUtils.deleteDirectory(swaggerOutputDir);
            FileUtils.forceDelete(docOutput);
        } catch (Exception e) {
            //ignore
        }

        File testPom = new File(getBasedir(), "target/test-classes/plugin-config-springmvc.xml");
        mojo = (ApiDocumentMojo) lookupMojo("generate", testPom);
    }

    @Test
    public void testGeneratedDoc() throws Exception {
        mojo.execute();

        final InputStream resource = getClass().getResourceAsStream("/sample-springmvc.html");
        final List<String> expect = IOUtils.readLines(resource);
        final List<String> testOutput = FileUtils.readLines(docOutput);

        Assert.assertEquals(expect.size(), testOutput.size());
        for (int i = 0; i < expect.size(); i++) {
            Assert.assertEquals(expect.get(i), testOutput.get(i));
        }
    }

    @Test
    public void testSwaggerOutput() throws Exception {
        mojo.execute();

        List<File> outputFiles = new ArrayList<File>();

        Collections.addAll(outputFiles, swaggerOutputDir.listFiles());
        Collections.sort(outputFiles);
        Assert.assertEquals(outputFiles.get(0).getName(), "car.json");
        Assert.assertEquals(outputFiles.get(1).getName(), "garage.json");
        Assert.assertEquals(outputFiles.get(2).getName(), "service.json");
        Assert.assertEquals(outputFiles.get(3).getName(), "v2");
        File v2 = outputFiles.get(3);
        Assert.assertTrue(v2.isDirectory());
        String[] v2carfile = v2.list();
        Assert.assertEquals(v2carfile[0], "car.json");


        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(FileUtils.readFileToByteArray(new File(swaggerOutputDir, "service.json")));

        JsonNode basePathNode = node.get("basePath");
        Assert.assertEquals(basePathNode.textValue(), "http://www.example.com/restapi/doc");

        JsonNode apis = node.get("apis");
        Assert.assertEquals(apis.size(), 3);
        List<String> pathInService = new ArrayList<String>();
        for (JsonNode api : apis) {
            pathInService.add(api.get("path").asText());
        }
        Collections.sort(pathInService);
        Assert.assertEquals(pathInService.get(0), "/car.{format}");
        Assert.assertEquals(pathInService.get(1), "/garage.{format}");
        Assert.assertEquals(pathInService.get(2), "/v2/car.{format}");
    }

    @Test
    public void testSwaggerOutputFlat() throws Exception {
        List<ApiSource> apisources = (List<ApiSource>) getVariableValueFromObject(mojo, "apiSources");
        apisources.get(0).setUseOutputFlatStructure(true);
        setVariableValueToObject(mojo, "apiSources", apisources);
        mojo.execute();

        List<String> flatfiles = new ArrayList<String>();

        Collections.addAll(flatfiles, swaggerOutputDir.list());
        Collections.sort(flatfiles);
        Assert.assertEquals(flatfiles.get(0), "car.json");
        Assert.assertEquals(flatfiles.get(1), "garage.json");
        Assert.assertEquals(flatfiles.get(2), "service.json");
        Assert.assertEquals(flatfiles.get(3), "v2_car.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(FileUtils.readFileToByteArray(new File(swaggerOutputDir, "service.json")));

        JsonNode basePathNode = node.get("basePath");
        Assert.assertEquals(basePathNode.textValue(), "http://www.example.com/restapi/doc");

        JsonNode apis = node.get("apis");
        Assert.assertEquals(apis.size(), 3);
        List<String> pathInService = new ArrayList<String>();
        for (JsonNode api : apis) {
            pathInService.add(api.get("path").asText());
        }
        Collections.sort(pathInService);
        Assert.assertEquals(pathInService.get(0), "/car.{format}");
        Assert.assertEquals(pathInService.get(1), "/garage.{format}");
        Assert.assertEquals(pathInService.get(2), "/v2_car.{format}");
    }

    @Test
    public void testSwaggerOutputFlatWithoutSwaggerUiPath() throws Exception {
        List<ApiSource> apisources = (List<ApiSource>) getVariableValueFromObject(mojo, "apiSources");
        apisources.get(0).setUseOutputFlatStructure(true);
        setVariableValueToObject(mojo, "apiSources", apisources);
        mojo.execute();

        List<String> flatfiles = new ArrayList<String>();

        Collections.addAll(flatfiles, swaggerOutputDir.list());
        Collections.sort(flatfiles);
        Assert.assertEquals(flatfiles.get(0), "car.json");
        Assert.assertEquals(flatfiles.get(1), "garage.json");
        Assert.assertEquals(flatfiles.get(2), "service.json");
        Assert.assertEquals(flatfiles.get(3), "v2_car.json");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(FileUtils.readFileToByteArray(new File(swaggerOutputDir, "service.json")));

        JsonNode basePathNode = node.get("basePath");
        Assert.assertEquals(basePathNode.textValue(), "http://example.com");

        JsonNode apis = node.get("apis");
        Assert.assertEquals(apis.size(), 3);
        List<String> pathInService = new ArrayList<String>();
        for (JsonNode api : apis) {
            pathInService.add(api.get("path").asText());
        }
        Collections.sort(pathInService);
        Assert.assertEquals(pathInService.get(0), "/car.{format}");
        Assert.assertEquals(pathInService.get(1), "/garage.{format}");
        Assert.assertEquals(pathInService.get(2), "/v2_car.{format}");
    }

    @Test
    public void testOverrideModels() throws MojoFailureException, MojoExecutionException, IOException {
        mojo.execute();
        File carfile = new File(swaggerOutputDir, "car.json");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode tree = objectMapper.readTree(carfile);
        Assert.assertEquals("Date in ISO-8601 format",
                tree.get("models").get("DateTime").get("properties")
                        .get("value").get("description").asText());

    }

    @DataProvider
    private Iterator<String[]> pathProvider() throws Exception {
        String tempDirPath = createTempDirPath();

        List<String[]> dataToBeReturned = new ArrayList<String[]>();
        dataToBeReturned.add(new String[]{ tempDirPath + "foo" + File.separator + "bar" + File
                .separator + "test.html" });
        dataToBeReturned.add(new String[]{ tempDirPath + File.separator + "bar" + File.separator +
                "test.html" });
        dataToBeReturned.add(new String[]{ tempDirPath + File.separator + "test.html" });
        dataToBeReturned.add(new String[]{ "test.html" });

        return dataToBeReturned.iterator();
    }

    @Test(dataProvider = "pathProvider", enabled = false)
    public void testExecuteDirectoryCreated(String path) throws Exception {

        mojo.getApiSources().get(0).setOutputFolder(path);

        File file = new File(path);
        mojo.execute();
        Assert.assertTrue(file.exists());
        if (file.getParentFile() != null) {
            FileUtils.deleteDirectory(file.getParentFile());
        }
    }

    @Test
    public void testTest() throws Exception {
        Class<?> clazz = PetController.class;
        Method m1 = clazz.getMethod("test");
        Method m2 = clazz.getMethod("test2");
        Method m3 = clazz.getMethod("test3");
        Method m4 = clazz.getMethod("test4");
        Method m5 = clazz.getMethod("test5");
        Method m6 = clazz.getMethod("test6");
        Method m7 = clazz.getMethod("test7");
        Method m8 = clazz.getMethod("test8");
        Method m9 = clazz.getMethod("test9");
        Method testMapMethod = clazz.getMethod("testMap", Map.class);

        TypeUtils.TypeHolder m1tree = TypeUtils.getFullTypesTree(clazz, m1.getGenericReturnType());
        TypeUtils.TypeHolder m2tree = TypeUtils.getFullTypesTree(clazz, m2.getGenericReturnType());
        TypeUtils.TypeHolder m3tree = TypeUtils.getFullTypesTree(clazz, m3.getGenericReturnType());
        TypeUtils.TypeHolder m4tree = TypeUtils.getFullTypesTree(clazz, m4.getGenericReturnType());
        TypeUtils.TypeHolder m5tree = TypeUtils.getFullTypesTree(clazz, m5.getGenericReturnType());
        TypeUtils.TypeHolder m6tree = TypeUtils.getFullTypesTree(clazz, m6.getGenericReturnType());
        TypeUtils.TypeHolder m7tree = TypeUtils.getFullTypesTree(clazz, m7.getGenericReturnType());
        TypeUtils.TypeHolder m8tree = TypeUtils.getFullTypesTree(clazz, m8.getGenericReturnType());
        TypeUtils.TypeHolder m9tree = TypeUtils.getFullTypesTree(clazz, m9.getGenericReturnType());
        TypeUtils.TypeHolder testMapMethod9tree = TypeUtils.getFullTypesTree(clazz, testMapMethod.getGenericReturnType());

        System.out.println(m1tree);
        System.out.println(m2tree);
        System.out.println(m3tree);
        System.out.println(m4tree);
        System.out.println(m5tree);
        System.out.println(m6tree);
        System.out.println(m7tree);
        System.out.println(m8tree);
        System.out.println(m9tree);
        System.out.println(testMapMethod9tree);
        System.out.println("-------------");

        Assert.assertEquals(m1tree.toString(), "List<Pet>");
        Assert.assertEquals(m2tree.toString(), "List<Number>");
        Assert.assertEquals(m3tree.toString(), "Long");
        Assert.assertEquals(m4tree.toString(), "Long");
        Assert.assertEquals(m5tree.toString(), "void");
        Assert.assertEquals(m6tree.toString(), "List<Set<Number>>");
        Assert.assertEquals(m7tree.toString(), "List<Collection<Number>>");
        Assert.assertEquals(m8tree.toString(), "List<Pet>");
        Assert.assertEquals(m9tree.toString(), "Set<List<Pet>>");
        Assert.assertEquals(testMapMethod9tree.toString(), "Map<User,List<DateTime>>");

        Assert.assertTrue(m7tree.collectAllClasses().contains(List.class));
        Assert.assertTrue(m7tree.collectAllClasses().contains(Collection.class));
        Assert.assertTrue(m7tree.collectAllClasses().contains(Number.class));
    }

    @Test
    public void testParseStringTypeHolder() {
        StringTypeHolder typeHolder = TypeUtils.parseClassNamesFromGenericString("Request<Map<String,List<User>>>");
        Assert.assertEquals("Request", typeHolder.getTypeName());

        List<StringTypeHolder> generics = typeHolder.getGenerics();

        Assert.assertEquals(1, generics.size());
        Assert.assertEquals(2, generics.get(0).getGenerics().size()); // map
        Assert.assertEquals("String", generics.get(0).getGenerics().get(0).getTypeName()); // maps key (String)
        Assert.assertEquals("List", generics.get(0).getGenerics().get(1).getTypeName()); // maps value (List)
        Assert.assertEquals(0, generics.get(0).getGenerics().get(0).getGenerics().size()); // string has no generics
        Assert.assertEquals(1, generics.get(0).getGenerics().get(1).getGenerics().size()); // lists generics (User)
        Assert.assertEquals("User", generics.get(0).getGenerics().get(1).getGenerics().get(0).getTypeName()); // lists generics (User)
    }

    @Test
    public void testComplexParseStringTypeHolder() {
        StringTypeHolder complexTypeHolder = TypeUtils.parseClassNamesFromGenericString("Map<List<Long>, Map<String, List<Set<String>>>>");
        List<String> allTypes = complexTypeHolder.collectAllTypes();
        Assert.assertEquals(8, allTypes.size());
        Assert.assertEquals("Map", allTypes.get(0));
        Assert.assertEquals("List", allTypes.get(1));
        Assert.assertEquals("Long", allTypes.get(2));
        Assert.assertEquals("Map", allTypes.get(3));
        Assert.assertEquals("String", allTypes.get(4));
        Assert.assertEquals("List", allTypes.get(5));
        Assert.assertEquals("Set", allTypes.get(6));
        Assert.assertEquals("String", allTypes.get(7));
    }

    private String createTempDirPath() throws Exception {
        File tempFile = File.createTempFile("swagmvn", "test");
        String path = tempFile.getAbsolutePath();
        FileUtils.deleteQuietly(tempFile);
        return path;
    }
}
