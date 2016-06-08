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

    @Test
    public void serializerSerializesMediatorTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED);

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
