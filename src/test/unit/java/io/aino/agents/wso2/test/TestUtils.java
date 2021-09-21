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

import io.aino.agents.core.Agent;
import io.aino.agents.core.config.AgentConfig;
import io.aino.agents.wso2.mediator.AinoMediator;
import io.aino.agents.wso2.mediator.factory.AinoMediatorFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.jaxen.JaxenException;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static String SYNAPSE_NAMESPACE = XMLConfigConstants.SYNAPSE_NAMESPACE;
     
    
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_STATUS = "/validAinoConfigAllElementsDynamicStatus.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_STATIC_FROM_STATIC_TO = "/validAinoConfigAllElementsStaticFomApplicatioAndStaticToApplication.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM_DYNAMIC_TO = "/validAinoConfigAllElementsDynamicFomApplicatioAndDynamicToApplication.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM_STATIC_TO = "/validAinoConfigAllElementsDynamicFomApplicatioAndStaticToApplication.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_TO = "/validAinoConfigAllElementsDynamicToApplication.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_FROM = "/validAinoConfigAllElementsDynamicFomApplication.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_DYNAMIC_MESSAGE = "/validAinoConfigAllElementsDynamicMessage.xml";
    public static String AINO_PROXY_CONFIG_REQUIRED_ELEMENTS = "/validAinoConfigRequiredElements.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS = "/validAinoConfigAllElements.xml";
    public static String AINO_PROXY_CONFIG_ALL_ELEMENTS_AND_PROPERTIES = "/validAinoConfigAllElementsAndProperties.xml";
    public static String AINO_PROXY_CONFIG_INVALID_APPLICATION_KEY = "/ainoConfigInvalidApplicationKey.xml";
    public static String AINO_PROXY_CONFIG_INVALID_OPERATION_KEY = "/ainoConfigInvalidOperationKey.xml";
    public static String AINO_PROXY_CONFIG_INVALID_ID_KEY = "/ainoConfigInvalidIdKey.xml";
    public static String AINO_PROXY_CONFIG_INVALID_PAYLOAD_TYPE_KEY = "/ainoConfigInvalidPayloadTypeKey.xml";
    public static String AINO_PROXY_MISSING_STATUS = "/ainoConfigInvalidMissingStatus.xml";
    public static String AINO_PROXY_MISSING_FROM_AND_TO = "/ainoConfigInvalidMissingFromAndTo.xml";
    public static String MESSAGE_ID = "123456789";

    public static AXIOMXPath ainoLogs;

    static {
        try {
            ainoLogs = new AXIOMXPath("//syn:ainoLog");
            ainoLogs.addNamespace("syn", SYNAPSE_NAMESPACE);
        } catch (JaxenException e) {
            e.printStackTrace();
        }
    }


    public static OMElement getDocumentElementFromResourcePath(String path) throws Exception {
        return new StAXOMBuilder(new FileInputStream(new File(AinoMediatorFactoryTest.class.getResource(path)
                .getPath()))).getDocumentElement();
    }

    public static Mediator createMockedAinoLogMediator(AinoMediatorFactory factory, String resource) throws Exception {
        AinoMediator m = (AinoMediator) createAinoLogMediator(factory, resource);

        // mock the agent but use the real config inside mocked agent
        AgentConfig config = m.ainoAgent.getAgentConfig();
        m.ainoAgent = mock(Agent.class);
        when(m.ainoAgent.getAgentConfig()).thenReturn(config);

        return m;
    }

    public static Mediator createAinoLogMediator(MediatorFactory factory, String resource) throws Exception {
        OMElement proxy = TestUtils.getDocumentElementFromResourcePath(resource);
        List<OMElement> ainoConfigs = (List<OMElement>) ainoLogs.evaluate(proxy);
        Mediator m = factory.createMediator(ainoConfigs.get(0), null);

        return m;
    }

    public static Axis2MessageContext getMockedContext() throws Exception {
        MessageContext axisCtx = mock(MessageContext.class);
        org.apache.synapse.core.axis2.Axis2MessageContext synapseCtx = mock(org.apache.synapse.core.axis2.Axis2MessageContext.class);
        when(synapseCtx.getAxis2MessageContext()).thenReturn(axisCtx);
        when(axisCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS)).thenReturn(new HashMap<String, String>());
        when(synapseCtx.getAxis2MessageContext().getMessageID()).thenReturn(MESSAGE_ID);
        return synapseCtx;
    }


}
