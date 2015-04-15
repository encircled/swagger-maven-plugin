package com.github.kongchen.jaxrs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class SwaggerMavenPluginTest extends AbstractMojoTestCase {

    private File swaggerOutputDir = new File(getBasedir(), "generated/swagger-ui");
    private File docOutput = new File(getBasedir(), "generated/document.html");
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

        File testPom = new File(getBasedir(), "target/test-classes/plugin-config.xml");
        mojo = (ApiDocumentMojo) lookupMojo("generate", testPom);
    }

    @Test
    public void testGeneratedDoc() throws Exception {
        mojo.execute();

        final InputStream resource = getClass().getResourceAsStream("/sample.html");
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
        Assert.assertEquals("Content-Type in HTTP request/response header",
                tree.get("models").get("MediaType").get("properties")
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

    @Test(dataProvider = "pathProvider")
    public void testExecuteDirectoryCreated(String path) throws Exception {

        mojo.getApiSources().get(0).setOutputFolder(path);

        File file = new File(path);
        mojo.execute();
        Assert.assertTrue(file.exists());
        if (file.getParentFile() != null) {
            FileUtils.deleteDirectory(file.getParentFile());
        }
    }

    private String createTempDirPath() throws Exception {
        File tempFile = File.createTempFile("swagmvn", "test");
        String path = tempFile.getAbsolutePath();
        tempFile.delete();
        return path;
    }
}
