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

import io.aino.agents.core.Transaction;
import io.aino.agents.wso2.mediator.AinoMediator;
import io.aino.agents.wso2.mediator.factory.AinoMediatorFactory;
import org.apache.axiom.om.*;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AinoMediatorTest {

    private static AinoMediatorFactory factory;

    public AinoMediatorTest() throws JaxenException {
    }


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

    }

    private List<OMNode> createXPathResultForIds() {
        List<OMNode> returnList = new ArrayList<OMNode>();

        OMElement elem1 = OMAbstractFactory.getOMFactory().createOMElement(new QName("dummy"));
        elem1.setText("123123");
        returnList.add(elem1);

        OMText elem2 = OMAbstractFactory.getOMFactory().createOMText("4441");
        returnList.add(elem2);

        return returnList;
    }

    private List<String> createExpectedIdList() {
        List<String> expectedIds = new ArrayList<String>();
        expectedIds.add("123123");
        expectedIds.add("4441");

        return expectedIds;
    }

    @Test
    public void mediatorUsesAinoLoggerWhenEnabledTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);
        Axis2MessageContext ctx = TestUtils.getMockedContext();

        when(m.ainoAgent.isEnabled()).thenReturn(true);
        when(m.ainoAgent.newTransaction()).thenReturn(new Transaction(null));

        m.mediate(ctx);

        verify(m.ainoAgent).addTransaction((Transaction) any());
    }

    @Test
    public void mediatorDoesNotUseAinoLoggerWhenNotEnabledTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);
        Axis2MessageContext ctx = TestUtils.getMockedContext();

        when(m.ainoAgent.isEnabled()).thenReturn(false);

        m.mediate(ctx);

        verify(m.ainoAgent, never()).addTransaction((Transaction) any());
    }

    @Test
    public void mediatorBuildsIdListProperlyTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);
        Axis2MessageContext ctx = TestUtils.getMockedContext();

        SynapseXPath idXPath = mock(SynapseXPath.class);
        when(idXPath.evaluate(ctx)).thenReturn(createXPathResultForIds());
        m.addId("typeKey", idXPath);

        when(m.ainoAgent.isEnabled()).thenReturn(true);
        when(m.ainoAgent.newTransaction()).thenReturn(new Transaction(null));

        m.mediate(ctx);

        ArgumentCaptor<Transaction> argument = ArgumentCaptor.forClass(Transaction.class);
        verify(m.ainoAgent, atLeastOnce()).addTransaction(argument.capture());
        List<String> actualIds = (List<String>) argument.getValue().getIds().values().toArray()[0];

        assertEquals(createExpectedIdList(), actualIds);
    }

    @Test
    public void mediatorBuildsIdListFromOneIdTest() throws Exception {
        AinoMediator m = (AinoMediator) TestUtils.createMockedAinoLogMediator(factory, TestUtils.AINO_PROXY_CONFIG_REQUIRED_ELEMENTS);
        Axis2MessageContext ctx = TestUtils.getMockedContext();

        SynapseXPath idXPath = mock(SynapseXPath.class);
        when(idXPath.evaluate(ctx)).thenReturn("9991");
        m.addId("typeKey", idXPath);

        when(m.ainoAgent.isEnabled()).thenReturn(true);
        when(m.ainoAgent.newTransaction()).thenReturn(new Transaction(null));

        m.mediate(ctx);

        ArgumentCaptor<Transaction> argument = ArgumentCaptor.forClass(Transaction.class);
        verify(m.ainoAgent, atLeastOnce()).addTransaction(argument.capture());
        List<String> actualIds = (List<String>) argument.getValue().getIds().values().toArray()[0];
        List<String> expected = new ArrayList<String>();
        expected.add("9991");

        assertEquals(expected, actualIds);
    }


}










