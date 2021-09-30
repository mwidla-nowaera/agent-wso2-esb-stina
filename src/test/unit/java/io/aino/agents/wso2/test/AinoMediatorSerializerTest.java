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

import io.aino.agents.core.config.InvalidAgentConfigException;
import io.aino.agents.wso2.mediator.AinoMediator;
import io.aino.agents.wso2.mediator.factory.AinoMediatorFactory;
import io.aino.agents.wso2.mediator.serializer.AinoMediatorSerializer;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AinoMediatorSerializerTest {

    private static AinoMediatorFactory factory;
    private AinoMediatorSerializer serializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        InputStream axisConf = new FileInputStream(new File(
                TestUtils.class.getResource("/conf/axis2.xml").getFile()));

        InputStream mediatorConf = new FileInputStream(new File(
                TestUtils.class.getResource("/conf/ainoLogMediatorConfig.xml").getFile()));

        factory = new AinoMediatorFactory(mediatorConf, axisConf);
    }

    @Before
    public void setUp() {
        serializer = new AinoMediatorSerializer();
    }

    // TODO The adding of dynamic status with statusExpression attribute. Has a bit o challenge on XSD level to check that status attribute relly exists.  
   // @Test(expected = InvalidAgentConfigException.class)
    public void serializerFailsToSerializeMediatoWithMissingElementTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_MISSING_STATUS);

        OMElement serializedMediator = serializer.serializeMediator(null, m);

        assertNotNull(serializedMediator);
    }

    @Test
    public void serializerSerializesMediatorTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);

        OMElement serializedMediator = serializer.serializeMediator(null, m);

        assertNotNull(serializedMediator);
    }

    @Test
    public void serializerSetFromApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS);

        OMAttribute attribute = serializeAndFindAttribute(m, "from", "applicationKey");

        assertEquals("app01", attribute.getAttributeValue());
    }

    @Test
    public void serializerSetOperationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS);

        OMAttribute attribute = serializeAndFindAttribute(m, "operation", "key");

        assertEquals("update", attribute.getAttributeValue());
    }

    @Test
    public void serializerSetMessageTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS);

        OMAttribute attribute = serializeAndFindAttribute(m, "message", "value");

        assertEquals("success", attribute.getAttributeValue());
    }


    @Test
    public void serializerSetDynamicOperationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_OPERATION);

        OMAttribute attribute = serializeAndFindAttribute(m, "operation", "expression");

        assertEquals("//order/operation", attribute.getAttributeValue());
    }

    
    @Test
    public void serializerSetDynamicFromApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM);

        OMAttribute attribute = serializeAndFindAttribute(m, "from", "expression");

        assertEquals("//order/from", attribute.getAttributeValue());
        // Check that the to Aplication is set as default esb
        assertEquals("esb", m.getToApplication());
    }

    @Test
    public void serializerSetDynamicToApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_TO);

        OMAttribute attribute = serializeAndFindAttribute(m, "to", "expression");

        assertEquals("//order/to", attribute.getAttributeValue());
        // Check that the from Aplication is set as default esb
        assertEquals("esb", m.getFromApplication());
    }

    @Test
    public void serializerSetDynamicFromStaticToApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM_STATIC_TO);

        OMAttribute attribute = serializeAndFindAttribute(m, "from", "expression");
        assertEquals("//order/from", attribute.getAttributeValue());

        OMAttribute toAttribute = serializeAndFindAttribute(m, "to", "applicationKey");
        assertEquals("app01", toAttribute.getAttributeValue());
    }

    @Test
    public void serializerSetStaticFromStaticToApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_STATIC_FROM_STATIC_TO);

        OMAttribute attribute = serializeAndFindAttribute(m, "from", "applicationKey");
        assertEquals("app01", attribute.getAttributeValue());

        OMAttribute toAttribute = serializeAndFindAttribute(m, "to", "applicationKey");
        assertEquals("app02", toAttribute.getAttributeValue());
    }
    
    @Test
    public void serializerSetDynamicFromDynamicToApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM_DYNAMIC_TO);

        OMAttribute attribute = serializeAndFindAttribute(m, "from", "expression");
        assertEquals("//order/from", attribute.getAttributeValue());
        OMAttribute toAttribute = serializeAndFindAttribute(m, "to", "expression");
        assertEquals("//order/to", toAttribute.getAttributeValue());
    }
        

    @Test
    public void serializerSetDynamicMessageTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_MESSAGE);

        OMAttribute attribute = serializeAndFindAttribute(m, "message", "expression");

        assertEquals("//order/orderId", attribute.getAttributeValue());
    }

    @Test
    public void serializerSetDynamicStatusTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_STATUS);

        OMElement serializedMediator = serializer.serializeMediator(null, m);
        assertNotNull(serializedMediator);
        OMElement operationElement = (OMElement) serializedMediator.getChildrenWithLocalName("message").next();
        assertNotNull(operationElement);
        OMElement parent = (OMElement) operationElement.getParent();
        assertNotNull(parent);
        assertEquals("//order/status", parent.getAttributeValue(new QName("statusExpression")));
    }

    

    @Test
    public void serializerSetToApplicationTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_ALL_ELEMENTS_AND_PROPERTIES);

        OMAttribute attribute = serializeAndFindAttribute(m, "to", "applicationKey");

        assertEquals("app02", attribute.getAttributeValue());
    }

    public OMAttribute serializeAndFindAttribute(Mediator m, String tagName, String attributeName) {
        OMElement serializedMediator = serializer.serializeMediator(null, m);
        assertNotNull(serializedMediator);

        OMElement operationElement = (OMElement) serializedMediator.getChildrenWithLocalName(tagName).next();
        assertNotNull(operationElement);

        OMAttribute attribute = operationElement.getAttribute(new QName(attributeName));
        assertNotNull(attribute);

        return attribute;
    }


}
