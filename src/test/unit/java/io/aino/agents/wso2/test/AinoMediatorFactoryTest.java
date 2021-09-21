/*
 *  Copyright 2016 Aino.io
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.aino.agents.wso2.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import io.aino.agents.wso2.mediator.AinoMediator;
import io.aino.agents.core.config.InvalidAgentConfigException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.LogMediator;
import org.jaxen.JaxenException;
import org.junit.*;

import io.aino.agents.wso2.mediator.factory.AinoMediatorFactory;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class AinoMediatorFactoryTest {

    private static final String SERVER_NAME_IN_AXIS2_CONFIG = "localhost_sweet_localhost";
    private static final String COMPUTER_NAME_PROPERTY = "COMPUTERNAME";
    private static final String HOST_NAME_PROPERTY = "HOSTNAME";

    private final AXIOMXPath ainoLogs;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();


    public AinoMediatorFactoryTest() throws FileNotFoundException, XMLStreamException, JaxenException {
        ainoLogs = new AXIOMXPath("//syn:ainoLog");
        ainoLogs.addNamespace("syn", TestUtils.SYNAPSE_NAMESPACE);
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        getFactory().clearAinoLogger();
    }

    private static final Map<Integer, String> LOG_CATEGORY_MAP = new HashMap<Integer, String>();

    static {
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_DEBUG, "DEBUG");
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_ERROR, "ERROR");
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_FATAL, "FATAL");
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_INFO, "INFO");
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_TRACE, "TRACE");
        LOG_CATEGORY_MAP.put(LogMediator.CATEGORY_WARN, "WARN");
    }

    private static final Map<Integer, String> LOG_LEVEL_MAP = new HashMap<Integer, String>();

    static {
        LOG_LEVEL_MAP.put(LogMediator.CUSTOM, "custom");
        LOG_LEVEL_MAP.put(LogMediator.FULL, "full");
        LOG_LEVEL_MAP.put(LogMediator.HEADERS, "headers");
        LOG_LEVEL_MAP.put(LogMediator.SIMPLE, "simple");
    }

    private AinoMediatorFactory getFactory() throws FileNotFoundException {
        return new AinoMediatorFactory(new FileInputStream(new File(AinoMediatorFactoryTest.class
                .getResource("/conf/ainoLogMediatorConfig.xml").getFile())), new FileInputStream(new File(
                AinoMediatorFactoryTest.class.getResource("/conf/axis2.xml").getFile())));
    }

    // TODO The adding of dynamic status with statusExpression attribute. Has a bit o challenge on XSD level to check that status attribute relly exists.  
   // @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithMissingRequiredStatusElement() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_MISSING_STATUS);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        AinoMediator m = (AinoMediator) getFactory().createMediator(ainoConfigs.get(0), null);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithMissingRequiredFromAndToElement() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_MISSING_FROM_AND_TO);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        AinoMediator m = (AinoMediator) getFactory().createMediator(ainoConfigs.get(0), null);
    }

    @Test
    public void testCreateMediatorWithRequiredElements() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        AinoMediator m = (AinoMediator) getFactory().createMediator(ainoConfigs.get(0), null);
        assertNotNull(m);
        assertNotNull(m.getStatus());
        assertNotNull(m.getFromApplication());
    }

    @Test
    public void testCreateMediatorWithAllElements() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        AinoMediator m = (AinoMediator) getFactory().createMediator(ainoConfigs.get(0), null);
        assertNotNull(m);
        assertNotNull(m.getStatus());
        assertNotNull(m.getFromApplication());
        assertNotNull(m.getOperation());
        assertNotNull(m.getPayloadType());
        assertNotNull(m.getIdList());
        assertNotNull(m.getMessage());
    }

    @Test
    public void testCreateMediatorWithAllElementsAndProperties() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_AND_PROPERTIES);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        AinoMediator m = (AinoMediator) getFactory().createMediator(ainoConfigs.get(0), null);

        assertNotNull(m);
        assertNotNull(m.getStatus());
        assertNotNull(m.getFromApplication());
        assertNotNull(m.getOperation());
        assertNotNull(m.getPayloadType());
        assertNotNull(m.getIdList());
        assertNotNull(m.getMessage());
        assertEquals(m.getProperties().size(), 3);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithInvalidApplicationKey() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_INVALID_APPLICATION_KEY);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);

        Mediator m = getFactory().createMediator(ainoConfigs.get(0), null);
        assertNull(m);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithInvalidOperationKey() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_INVALID_OPERATION_KEY);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);

        Mediator m = getFactory().createMediator(ainoConfigs.get(0), null);
        assertNull(m);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithInvalidIdKey() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_INVALID_ID_KEY);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);

        Mediator m = getFactory().createMediator(ainoConfigs.get(0), null);
        assertNull(m);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testCreateMediatorWithInvalidPayloadTypeKey() throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(TestUtils.AINO_PROXY_CONFIG_INVALID_PAYLOAD_TYPE_KEY);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);

        Mediator m = getFactory().createMediator(ainoConfigs.get(0), null);
        assertNull(m);
    }

    @Test(expected = InvalidAgentConfigException.class)
    public void testInvalidFactoryConfig() throws FileNotFoundException {
        factoryFrom("ainoLogMediatorConfigIllegalCharacter.xml");
    }

    @Test
    public void testDisabledFactoryConfig() throws FileNotFoundException {
        // test fix for https://github.com/Aino-io/agent-wso2-esb/issues/22 :
        // factory creation should not fail when sender is disabled
        assertNotNull(factoryFrom("ainoLogMediatorConfigDisabled.xml"));
    }

    @Test
    public void testReadEsbServerNameFromComputerNameSystemProperty() throws FileNotFoundException {
        environmentVariables.set(COMPUTER_NAME_PROPERTY, "foo");
        environmentVariables.set(HOST_NAME_PROPERTY, "bar");
        AinoMediatorFactory mediatorFactory = factoryFrom("ainoLogMediatorConfig.xml");
        assertEquals("foo", mediatorFactory.getEsbServerName());
    }

    @Test
    public void testReadEsbServerNameFromHostNameSystemProperty() throws FileNotFoundException {
        environmentVariables.set(HOST_NAME_PROPERTY, "bar");
        AinoMediatorFactory mediatorFactory = factoryFrom("ainoLogMediatorConfig.xml");
        assertEquals("bar", mediatorFactory.getEsbServerName());
    }

    @Test
    public void testReadEsbServerNameFromAxis2Config() throws FileNotFoundException {
        // no environment variables set
        AinoMediatorFactory mediatorFactory = factoryFrom("ainoLogMediatorConfig.xml");
        assertEquals(SERVER_NAME_IN_AXIS2_CONFIG, mediatorFactory.getEsbServerName());
    }

    private AinoMediatorFactory factoryFrom(String ainoLogMediatorConfigFileName) throws FileNotFoundException {
        InputStream axisConf = new FileInputStream(new File(
                TestUtils.class.getResource("/conf/axis2.xml").getFile()));

        InputStream ainoConf = new FileInputStream(new File(
                TestUtils.class.getResource("/conf/" + ainoLogMediatorConfigFileName).getFile()));
        return new AinoMediatorFactory(ainoConf, axisConf);
    }
}
