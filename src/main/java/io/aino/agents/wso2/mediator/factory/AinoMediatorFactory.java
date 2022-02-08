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

package io.aino.agents.wso2.mediator.factory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import io.aino.agents.core.Agent;
import io.aino.agents.core.config.InvalidAgentConfigException;
import io.aino.agents.core.config.InputStreamConfigBuilder;

import io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.Deprecated;
import io.aino.agents.wso2.mediator.AinoMediator;
import io.aino.agents.wso2.mediator.util.Enum;
import io.aino.agents.wso2.mediator.util.MediatorLocation;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.*;

import org.apache.axiom.attachments.utils.IOUtils;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.MediatorPropertyFactory;
import org.apache.synapse.config.xml.SynapseXPathFactory;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Mediator factory for creating aino.io WSO2 ESB mediator from XML configurations.
 */
public class AinoMediatorFactory extends AbstractMediatorFactory {
    private static final Log log = LogFactory.getLog(AinoMediatorFactory.class);

    private static final String HOSTNAME_XPATH_STRING = "/axisconfig/parameter[@name = 'SynapseConfig.ServerName']/text()";

    private String esbServerName;
    private final static Lock ainoInitLock = new ReentrantLock();

    private static Agent ainoAgent;

    /**
     * This no-args constructor can be called only when the CARBON_HOME
     * environment variable is set.
     *
     * This constructor is called by the ESB when instantiating this class.
     *
     * @throws FileNotFoundException if config files are not found
     */
    public AinoMediatorFactory() throws FileNotFoundException {
        
        this(new FileInputStream(new File(AINO_CONFIG_FILE_PATH)),
                new FileInputStream(new File(AXIS2_CONFIG_FILE_PATH)));
    }

    public void clearAinoLogger() {
        ainoAgent = null;
    }

    /**
     * Constructor for creating mediator from specific config files.
     *
     * @param configFileInputStream aino.io config file as InputStream
     * @param axis2ConfigFileInputStream axis2 config file as InputStream
     */
    @SuppressWarnings("unchecked")
    public AinoMediatorFactory(InputStream configFileInputStream, InputStream axis2ConfigFileInputStream) {
        try {
            ByteArrayInputStream confStream = new ByteArrayInputStream(IOUtils.getStreamAsByteArray(configFileInputStream));

            esbServerName = getHostName(axis2ConfigFileInputStream);

            initializeAinoAgent(confStream);

        } catch (OMException e) {
            StringBuilder sb = new StringBuilder("Unable to read the aino config file. ");
            sb.append("The file either contains disallowed characters (&|<) that need to be escaped (&amp; and &lt; respectively) or is not well-formed.");
            throw new InvalidAgentConfigException(sb.toString(), e);
        } catch (IOException e) {
            throw new InvalidAgentConfigException("Failed to validate the Aino config file.", e);
        } finally {
            closeStreamQuietly(configFileInputStream);
            closeStreamQuietly(axis2ConfigFileInputStream);
        }
    }

    /**
     * Returns the ESB server name property.
     */
    public String getEsbServerName() {
        return esbServerName;
    }

    private boolean closeStreamQuietly(InputStream stream){
        if(null == stream){
            return true;
        }

        try {
            stream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void initializeAinoAgent(ByteArrayInputStream confStream) {
        if(ainoInitLock.tryLock()) {
            try {
                if(ainoAgent != null) return;
                confStream.reset();
                ainoAgent = Agent.getFactory().setConfigurationBuilder(new InputStreamConfigBuilder(confStream)).build();
                if(ainoAgent.isEnabled() && !ainoAgent.applicationExists("esb")) {
                    throw new InvalidAgentConfigException("application with key 'esb' must be configured.");
                }
            } finally {
                ainoInitLock.unlock();
            }
        }
    }


    @Override
    public QName getTagQName() {
        return ROOT_TAG;
    }

    @Override
    protected Mediator createSpecificMediator(OMElement element, Properties properties) {
        try {
            validateMediatorConfig(element);
        } catch (Exception e) {
            throw new InvalidAgentConfigException("Failed to validate ainoLog element", e);
        }

        AinoMediator mediator = new AinoMediator(MediatorLocation.getMediatorLocation(element), ainoAgent);

        mediator.setSeparator(element.getAttributeValue(ATT_SEPARATOR_Q));

        // required elements
        setMediatorStatus(element, mediator);
        setMediatorApplications(element, mediator);

        // optional elements
        setMediatorOperation(element, mediator);
        setMediatorMessage(element, mediator);
        setMediatorIds(element, mediator);
        setMediatorMultiids(element, mediator);
        setMediatorPayloadType(element, mediator);

        mediator.setEsbServerName(esbServerName);

        mediator.setProperties(MediatorPropertyFactory.getMediatorProperties(element));

        return mediator;
    }

    private void setMediatorPayloadType(OMElement element, AinoMediator mediator) {
        OMElement payloadTypeElement = element.getFirstChildWithName(PAYLOAD_Q);
        if (payloadTypeElement != null) {
            String payloadTypeKey = payloadTypeElement.getAttributeValue(ATT_PAYLOAD_TYPE_KEY_Q);
            if (payloadTypeKey != null) {
                if (!ainoAgent.payloadTypeExists(payloadTypeKey)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid ").append(ATT_KEY).append(" attribute of ").append(payloadTypeKey).append(" at ");
                    sb.append(PAYLOAD_Q).append(" element.");
                    sb.append("Valid values are specified in the Aino.io configuration file.");

                    throw new InvalidAgentConfigException(sb.toString());
                } else {
                    mediator.setPayloadType(payloadTypeKey);
                }
            }
            try {
                if (payloadTypeElement.getAttributeValue(ATT_EXPRN) != null) {
                    mediator.setDynamicPayloadType(SynapseXPathFactory.getSynapseXPath(payloadTypeElement, ATT_EXPRN));
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
                sb.append(PAYLOAD_Q).append(" element");
                throw new InvalidAgentConfigException(sb.toString(), e);
            }
        }
    }

    private void setMediatorApplications(OMElement element, AinoMediator mediator) {
        OMElement fromElement = element.getFirstChildWithName(FROM_Q);
        OMElement toElement = element.getFirstChildWithName(TO_Q);
        boolean bothExistsInConfig = fromElement != null && toElement != null;
        if (fromElement == null && toElement == null) {
            throw new InvalidAgentConfigException("From or To must be defined for Aino.io log");
        }
        if (fromElement != null) {
            configureApplicationDirection(Enum.ApplicationDirection.FROM, mediator, fromElement, bothExistsInConfig);
        }  
        if (toElement != null) {
            configureApplicationDirection(Enum.ApplicationDirection.TO, mediator, toElement, bothExistsInConfig);
        } 
    }

    private void configureApplicationDirection(Enum.ApplicationDirection direction, AinoMediator mediator, OMElement element, Boolean bothExistsInConfig) {
        String applicationKey = element.getAttributeValue(ATT_APPLICATION_KEY_Q);
        if (applicationKey != null){
            if (!ainoAgent.applicationExists(applicationKey)) {
                throw new InvalidAgentConfigException("application does not exist in config: " + applicationKey);
            }
            mediator.setApplication(direction, applicationKey);
        }
        try {
            if (element.getAttributeValue(ATT_EXPRN) != null) {
                mediator.setDynamicApplication(direction, SynapseXPathFactory.getSynapseXPath(element, ATT_EXPRN));
            }
        } catch (JaxenException e) {
            StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
            sb.append(MESSAGE_Q).append(" element");
            throw new InvalidAgentConfigException(sb.toString(), e);
        }

//        setPayloadTypeFromDeprecatedSpecifier(direction, mediator, element);
        if (bothExistsInConfig == false) {
            // Only set the other direction as ESB if only one direction was defined on the configs
            mediator.setApplication(direction.oppositeDirection(), "esb");
        }
    }

    @java.lang.Deprecated //Deprecated in v1.9.4
    private void setPayloadTypeFromDeprecatedSpecifier(Enum.ApplicationDirection direction, AinoMediator mediator, OMElement element) {
        String specifierKey = element.getAttributeValue(Deprecated.ATT_APP_SPECIFIER_KEY_Q);
        if(StringUtils.isBlank(specifierKey)){
            return;
        }

        log.warn("Attribute 'specifierKey' is deprecated and will be removed in future versions. Use 'payloadType' elements instead.");

        if (!ainoAgent.payloadTypeExists(specifierKey)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid ").append(Deprecated.APP_SPECIFIER_KEY_ATT_NAME).append(" attribute of ").append(specifierKey).append(" at ");
            sb.append(direction == Enum.ApplicationDirection.FROM ? FROM_Q : TO_Q).append(" element.");
            sb.append("Valid values are specified in the Aino.io configuration file.");
            sb.append("\n\nNB: Attribute 'specifierKey' is deprecated and will be removed in future versions. Use 'payloadType' elements instead.");

            throw new InvalidAgentConfigException(sb.toString());
        }

        mediator.setPayloadType(specifierKey);
    }

    private void setMediatorIds(OMElement element, AinoMediator mediator) {
        @SuppressWarnings("unchecked")
        Iterator<OMElement> idsElements = element.getChildrenWithName(IDS_Q);

        while (idsElements.hasNext()) {
            OMElement idsElement = idsElements.next();
            String typeKey = idsElement.getAttributeValue(ATT_TYPE_Q);

            if (!ainoAgent.getAgentConfig().getIdTypes().entryExists(typeKey)) {
                StringBuilder sb = new StringBuilder("An invalid id key has been given to a AinoMediator ");
                sb.append(IDS_Q).append(" element");
                throw new InvalidAgentConfigException(sb.toString());
            }

            try {
                mediator.addId(typeKey, SynapseXPathFactory.getSynapseXPath(idsElement, ATT_EXPRN));
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
                sb.append(IDS_Q).append(" element");
                throw new InvalidAgentConfigException(sb.toString(), e);
            }
        }
    }

    private void setMediatorMultiids(OMElement element, AinoMediator mediator) {
        OMElement multiidsElement = element.getFirstChildWithName(MULTIIDS_Q);
        if (multiidsElement != null) {
            String multiidsValue = multiidsElement.getAttributeValue(ATT_VALUE);
            if (multiidsValue != null) {
                mediator.setMultiids(multiidsValue);
                //  if (!ainoAgent.getAgentConfig().getIdTypes().entryExists(typeKey)) {
                //     StringBuilder sb = new StringBuilder();
                //     sb.append("Invalid ").append(ATT_VALUE).append(" attribute of ").append(multiidsValue).append(" at ");
                //     sb.append(MULTIIDS_Q).append(" element.");
                //     sb.append("Valid values are specified in the Aino.io configuration file.");

                //     throw new InvalidAgentConfigException(sb.toString());
                // } else {
                //     mediator.setMultiids(multiidsValue);
                // }
            }
            try {
                if (multiidsElement.getAttributeValue(ATT_EXPRN) != null) {
                    mediator.setDynamicMultiids(SynapseXPathFactory.getSynapseXPath(multiidsElement, ATT_EXPRN));
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
                sb.append(MULTIIDS_Q).append(" element");
                throw new InvalidAgentConfigException(sb.toString(), e);
            }
        }
    }

    private void setMediatorStatus(OMElement element, AinoMediator mediator) {
        try {
            String statusValue = element.getAttributeValue(ATT_STATUS_Q);   
            if (statusValue != null){
                mediator.setStatus(statusValue);
            }
            // So if for some reason status and statusExpression is placed. statusExpression is ovecrriding and used 
            if (element.getAttributeValue(ATT_STATUS_EXPRESSION_Q) != null) {
                mediator.setDynamicStatus(SynapseXPathFactory.getSynapseXPath(element, ATT_STATUS_EXPRESSION_Q));
            }
        } catch (JaxenException e) {
            StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
            sb.append(MESSAGE_Q).append(" element");
            throw new InvalidAgentConfigException(sb.toString(), e);
        }
    }

    private void setMediatorMessage(OMElement element, AinoMediator mediator) {
        OMElement messageElement = element.getFirstChildWithName(MESSAGE_Q);
        if (messageElement == null) { return; }
        if (messageElement.getAttributeValue(ATT_VALUE) != null){
            mediator.setMessage(messageElement.getAttributeValue(ATT_VALUE));
        }
        try {
            if (messageElement.getAttributeValue(ATT_EXPRN) != null) {
                mediator.setDynamicMessage(SynapseXPathFactory.getSynapseXPath(messageElement, ATT_EXPRN));
            }
        } catch (JaxenException e) {
            StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
            sb.append(MESSAGE_Q).append(" element");
            throw new InvalidAgentConfigException(sb.toString(), e);
        }
    }

    private void setMediatorOperation(OMElement element, AinoMediator mediator) {
        OMElement operationElement = element.getFirstChildWithName(OPERATION_Q);
        if (operationElement != null) {
            String operationKey = operationElement.getAttributeValue(ATT_KEY);
            if (operationKey != null) {
                if (!ainoAgent.operationExists(operationKey)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid ").append(ATT_KEY).append(" attribute of ").append(operationKey).append(" at ");
                    sb.append(OPERATION_Q).append(" element.");
                    sb.append("Valid values are specified in the Aino.io configuration file.");

                    throw new InvalidAgentConfigException(sb.toString());
                } else {
                    mediator.setOperation(operationKey);
                }
            }
            try {
                if (operationElement.getAttributeValue(ATT_EXPRN) != null) {
                    mediator.setDynamicOperation(SynapseXPathFactory.getSynapseXPath(operationElement, ATT_EXPRN));
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("An invalid xPath expression has been given to a AinoMediator ");
                sb.append(MESSAGE_Q).append(" element");
                throw new InvalidAgentConfigException(sb.toString(), e);
            }
        }

    }

    private void validateMediatorConfig(OMElement element) throws SAXException, IOException {
        validateXml(element, "/schemas/ainoLog.xsd");
    }

    private void validateXml(OMElement element, String schemaPath) throws SAXException, IOException {
        OMFactory doomFactory = DOOMAbstractFactory.getOMFactory();

        StAXOMBuilder doomBuilder = new StAXOMBuilder(doomFactory, element.getXMLStreamReader());

        Element domElement = (Element) doomBuilder.getDocumentElement();

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source source = new StreamSource(this.getClass().getResourceAsStream(schemaPath));

        Schema schema = factory.newSchema(source);

        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(domElement));
    }

    private static String getHostName(InputStream axis2ConfigInputStream) {
        String hostName = System.getenv("COMPUTERNAME");
        if (StringUtils.isNotEmpty(hostName)) {
            return hostName;
        }

        hostName = System.getenv("HOSTNAME");
        if (StringUtils.isNotEmpty(hostName)) {
            return hostName;
        }

        hostName = getHostNameFromAinoConfig(axis2ConfigInputStream);
        if (StringUtils.isNotEmpty(hostName)) {
            return hostName;
        }

        hostName = getInetHostName();
        if (StringUtils.isNotEmpty(hostName)) {
            return hostName;
        }

        return "localhost";
    }

    private static String getHostNameFromAinoConfig(InputStream axis2ConfigInputStream) {
        try {
            OMElement ainoConfigElement = new StAXOMBuilder(axis2ConfigInputStream).getDocumentElement();
            AXIOMXPath hostNameXpath = new AXIOMXPath(HOSTNAME_XPATH_STRING);
            return hostNameXpath.stringValueOf(ainoConfigElement);
        } catch (XMLStreamException e) {
            return null;
        } catch (JaxenException e) {
            return null;
        }
    }

    private static String getInetHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}