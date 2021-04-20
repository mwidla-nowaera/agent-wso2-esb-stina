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

package io.aino.agents.wso2.mediator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import io.aino.agents.core.Agent;
import io.aino.agents.core.Transaction;
import io.aino.agents.core.config.InvalidAgentConfigException;
import io.aino.agents.wso2.mediator.util.Enum;
import io.aino.agents.wso2.mediator.util.Id;
import io.aino.agents.wso2.mediator.util.IdPropertyBuilder;
import io.aino.agents.wso2.mediator.util.MediatorLocation;

import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.*;

/**
 * Aino.io WSO2 ESB mediator.
 */
public class AinoMediator extends AbstractMediator {

    public Agent ainoAgent;

    private String separator;
    private String operation;
    private String message;
    private SynapseXPath dynamicMessage = null;
    private String esbServerName;
    private String fromApplication;
    private String toApplication;
    private String payloadType;
    private Enum.Status status;

    private final MediatorLocation mediatorLocation;

    private List<MediatorProperty> customProperties;
    private final List<Id> idList = new ArrayList<Id>();


    @SuppressWarnings("serial")
    private class DataFieldList extends ArrayList<String> {

        public DataFieldList(List<String> data) {
            this.addAll(data);
        }

        public boolean doesNotContain(String item) {
            return !this.contains(item);
        }
    }

    private DataFieldList dataFields = new DataFieldList(Arrays.asList(
            "from",
            "to",
            "message",
            "status",
            "timestamp",
            "operation",
            "ids",
            "flowId",
            "payloadType"
    ));

    /**
     * Constructor.
     *
     * @param ml mediator location
     * @param agent aino agent instance
     */
    public AinoMediator(MediatorLocation ml, Agent agent) {
        this.mediatorLocation = ml;
        this.ainoAgent = agent;

    }

    private static MediatorProperty getMediatorProperty(String name, String value, String expression)
            throws JaxenException {
        MediatorProperty mp = new MediatorProperty();
        mp.setName(name);
        mp.setValue(value);

        if(null == expression){
            return mp;
        }

        invokePropertyExpression(expression, mp);
        return mp;
    }

    private static Object invokePropertyExpression(String expression, MediatorProperty mp) {
        try {
            // Workaround for a peculiar WSO2 update approach where an
            // intermediate (SynapsePath) class was added into the class
            // hierarchy
            Class<?> parameterClass = getParameterClass();
            Class<?> synapseXPathClass = getSynapseXpathClass();
            Method setExpression = MediatorProperty.class.getMethod("setExpression", parameterClass);
            return setExpression.invoke(mp, synapseXPathClass.getConstructor(String.class).newInstance(expression));
        } catch (Exception e) {
            throw new InvalidAgentConfigException(
                    "Unable to initialize a AinoMediator due to a reflection-related exception.", e);
        }
    }

    private static Class<?> getSynapseXpathClass(){
        return getClassForName("org.apache.synapse.util.xpath.SynapseXPath");
    }

    private static Class<?> getParameterClass(){
        return getClassForName(getParameterClassName());
    }

    private static Class<?> getClassForName(String name){
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not instantiate class: " + name);
        }
    }

    private static String getParameterClassName(){
        String esb480OrLater = "org.apache.synapse.config.xml.SynapsePath";
        String beforeEsb480 = "org.apache.synapse.util.xpath.SynapseXPath";
        try {
            // If running in ESB v4.8.0 or newer
            Class.forName(esb480OrLater);
            return esb480OrLater;
        } catch (ClassNotFoundException e) {
            // If running in ESB older than v4.8.0
            return beforeEsb480;
        }
    }

    /**
     * Adds id xpath with id type key.
     *
     * @param typeKey type key
     * @param xPath xpath of ids
     */
    public void addId(String typeKey, SynapseXPath xPath) {
        idList.add(new Id(typeKey, xPath));
    }

    /**
     * Adds custom list of {@link MediatorProperty}.
     * They will be logged by ESB.
     *
     * @param properties properties to add
     */
    public void setProperties(List<MediatorProperty> properties) {
        customProperties = properties;
    }

    /**
     * Gets all custom {@link MediatorProperty}.
     *
     * @return custom properties
     */
    public List<MediatorProperty> getProperties() {
        return customProperties;
    }

    @Override
    public boolean mediate(MessageContext context) {
        try {
            initTransportHeadersMap(context);


            Transaction transaction = createTransaction(context);
            new IdPropertyBuilder(this.idList).buildToContext(context, transaction);
            processTransaction(context, transaction);
            logToEsb(context, transaction);
        } catch (Exception e) {
            log.error("Error occurred while trying to log to aino.io!", e);
        }

        return true;
    }

    private void logToEsb(MessageContext context, Transaction transaction) {
        StringBuilder sb = new StringBuilder();

        for (MediatorProperty prop : customProperties) {
            sb.append(prop.getName()).append(" = ").append(prop.getValue());
            sb.append(this.separator);
        }

        if(transaction != null) {
            appendNormalFieldsToLogMessage(transaction, sb);
            appendIdsToLogMessage(transaction, sb);
        }

        log.info(sb.toString());
    }

    private void appendIdsToLogMessage(Transaction transaction, StringBuilder sb) {
        sb.append("ids = [");
        for(String idName : transaction.getIds().keySet()) {
            sb.append(idName).append(": [");
            List<String> ids = transaction.getIds().get(idName);
            sb.append(StringUtils.join(ids, ",")).append("],");
        }
        sb.append("]");
    }

    private void appendNormalFieldsToLogMessage(Transaction transaction, StringBuilder sb) {
        appendNameAndValueToLogMessage("operation",ainoAgent.getAgentConfig().getOperations().getEntry(transaction.getOperationKey()), sb);
        appendNameAndValueToLogMessage("flowId", transaction.getFlowId(), sb);
        appendNameAndValueToLogMessage("message", transaction.getMessage(), sb);
        appendNameAndValueToLogMessage("status", transaction.getStatus(), sb);
        appendNameAndValueToLogMessage("payloadType", ainoAgent.getAgentConfig().getPayloadTypes().getEntry(transaction.getPayloadTypeKey()), sb);
        appendNameAndValueToLogMessage("from", ainoAgent.getAgentConfig().getApplications().getEntry(transaction.getFromKey()), sb);
        appendNameAndValueToLogMessage("to", ainoAgent.getAgentConfig().getApplications().getEntry(transaction.getToKey()), sb);
        appendNameAndValueToLogMessage("ainoTimestamp", String.valueOf(transaction.getTimestamp()), sb);
    }

    private void appendNameAndValueToLogMessage(String name, String value, StringBuilder sb) {
        sb.append(name)
                .append(" = ")
                .append(value)
                .append(this.separator);
    }


    private void processTransaction(MessageContext context, Transaction transaction) {
        if(transaction == null) { return; }
        addFieldsToTransaction(transaction);
        // If message is given as dynamic expression. Process it here. 
        String calculatedMessage = processDynamicMessage(context);
        if (calculatedMessage != null) {
            transaction.setMessage(calculatedMessage);
        } else {
            // Static value attribute was used 
            transaction.setMessage(this.message);
        }
            

        for (MediatorProperty property : customProperties) {
            if (isMetadataProperty(property)) {
                String propertyValue = property.getValue() != null ? property.getValue() : property.getEvaluatedExpression(context);
                transaction.addMetadata(property.getName(), propertyValue);
            }
        }

        ainoAgent.addTransaction(transaction);
    }

    private boolean isMetadataProperty(MediatorProperty prop) {
        if(prop == null)
            return false;

        return this.dataFields.doesNotContain(prop.getName());
    }

    private void addFieldsToTransaction(Transaction transaction) {
        transaction.setFromKey(this.fromApplication);
        transaction.setToKey(this.toApplication);
        transaction.setStatus(this.status == null ? "" : this.status.toString());
        transaction.setPayloadTypeKey(this.payloadType);
    }

    private Transaction createTransaction(MessageContext context) {

        String flowId = validateOrSetAinoFlowId(context);
        String operationKey = validateOrSetAinoOperationName(context);

        Transaction transaction = null;
        if (ainoAgent.isEnabled()) {
            transaction = ainoAgent.newTransaction();

            transaction.setFlowId(flowId);
            transaction.setOperationKey(operationKey);

            transaction.addMetadata("artifactName", mediatorLocation.getArtifactName());
            transaction.addMetadata("esbServerName", esbServerName);
            transaction.addMetadata("artifactType", mediatorLocation.getArtifactType());
            transaction.addMetadata("artifactName", mediatorLocation.getArtifactName());
            transaction.addMetadata("lineNumber", Integer.toString(mediatorLocation.getLineNumber()));

            if (status == Enum.Status.FAILURE) {
                addErrorMetadata(context, transaction);
            }
        }

        return transaction;
    }

    private void addErrorMetadata(MessageContext context, Transaction transaction) {
        if (context.getProperty("ERROR_CODE") != null) {
            transaction.addMetadata("errorCode", context.getProperty("ERROR_CODE").toString());
        }

        if (context.getProperty("ERROR_MESSAGE") != null) {
            transaction.addMetadata("errorMessage", context.getProperty("ERROR_MESSAGE").toString());
        }

        if (context.getProperty("ERROR_DETAIL") != null) {
            transaction.addMetadata("errorDetails", context.getProperty("ERROR_DETAIL").toString());
        }

        if (context.getProperty("ERROR_EXCEPTION") != null) {
            transaction.addMetadata("errorException", context.getProperty("ERROR_EXCEPTION").toString());
        }
    }

    /**
     * Creates a transport headers map if one does not exist.
     *
     * @param context
     */
    private Map<String,String> initTransportHeadersMap(MessageContext context) {
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
        Map<String,String> headersMap = (Map<String, String>) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headersMap == null) {
            headersMap = new HashMap<String, String>();
            context.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
        }
        return headersMap;
    }

    private String validateOrSetAinoOperationName(MessageContext context) {
        String operationKey;

        if(null != operation) {
            operationKey = operation;
        } else {
            operationKey = getOperationFromHeadersAndContext(context);
        }

        setPropertyToTransportHeadersMap(context, AINO_OPERATION_KEY_PROPERTY_NAME,  operationKey);

        return operationKey;
    }

    private String getOperationFromHeadersAndContext(MessageContext context) {
        String ainoOperationName = getOperationNameFromTransportHeaders(context);

        if (null == ainoOperationName) {
            ainoOperationName = getOperationNameFromMessageContext(context);
        }

        return ainoOperationName;
    }

    private String getOperationNameFromTransportHeaders(MessageContext context){
        // This logic is in place for situations where the message is coming back from a system which doesn't return custom transport headers.
        try {
            Map<String, String> headersMap = getTransportHeadersMap(context);
            return headersMap.get(AINO_OPERATION_KEY_PROPERTY_NAME);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private String getOperationNameFromMessageContext(MessageContext context){
        try {
            String name = (String) context.getProperty(AINO_OPERATION_KEY_PROPERTY_NAME);
            return name;
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private void setPropertyToTransportHeadersMap(MessageContext context, String key, String value) {
        Map<String, String> headersMap = getTransportHeadersMap(context);
        headersMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getTransportHeadersMap(MessageContext context) {
        return initTransportHeadersMap(context);
    }


    private String validateOrSetAinoFlowId(MessageContext context) {
        Map<String, String> headersMap = getTransportHeadersMap(context);

        String flowId = headersMap.get(AINO_FLOW_ID_PROPERTY_NAME);

        if(null == flowId){
            flowId = getFlowIdFromMessageContext(context);
        }

        if(null == flowId){
            flowId = ((Axis2MessageContext) context).getAxis2MessageContext().getMessageID();
        }

        setPropertyToTransportHeadersMap(context, AINO_FLOW_ID_PROPERTY_NAME, flowId);

        return flowId;
    }

    private String getFlowIdFromMessageContext(MessageContext context) {
        try {
            String flowId = (String) context.getProperty(AINO_FLOW_ID_PROPERTY_NAME);

            return flowId;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Gets mediator location.
     *
     * @return mediator location
     */
    public MediatorLocation getMediatorLocation() {
        return mediatorLocation;
    }


    /**
     * Gets separator used in logging.
     * Separator is used in between properties.
     *
     * @return separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Sets separator used in logging.
     *
     * @param separator separator
     */
    public void setSeparator(String separator) {
        if (StringUtils.isEmpty(separator)) {
            separator = ",";
        }
        this.separator = separator;
    }

    /**
     * Gets operation key configured to this mediator.
     *
     * @return operation key
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Sets operation key.
     *
     * @param operation operation key
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * Gets message configured to this mediator.
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets message.
     *
     * @param message message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public void setDynamicMessage(SynapseXPath xpath){
        this.dynamicMessage = xpath;
    }

    public  SynapseXPath getDynamicMessage(){
        return this.dynamicMessage;
    }

    protected String processDynamicMessage(MessageContext context){
        if (this.dynamicMessage != null) {
            try {
                Object evaluationResult = dynamicMessage.evaluate(context);
                if (evaluationResult != null){
                    return evaluationResult.toString();
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic message");
                sb.append(" XPath expression: ").append(dynamicMessage.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
        }
        return null;
    }

    /**
     * Gets the server name of ESB.
     *
     * @return name of server
     */
    public String getEsbServerName() {
        return esbServerName;
    }

    /**
     * Sets the server name of ESB.
     *
     * @param esbServerName server name
     */
    public void setEsbServerName(String esbServerName) {
        this.esbServerName = esbServerName;
    }

    /**
     * Gets configured 'to application'.
     *
     * @return to applcation key
     */
    public String getFromApplication() {
        return fromApplication;
    }

    /**
     * Sets 'from' application.
     *
     * @param fromApplication from application key
     */
    public void setFromApplication(String fromApplication) {
        this.fromApplication = fromApplication;
    }

    /**
     * Gets configured 'to' application.
     *
     * @return to application key
     */
    public String getToApplication() {
        return toApplication;
    }

    /**
     * Sets 'to' application.
     *
     * @param toApplication to application key
     */
    public void setToApplication(String toApplication) {
        this.toApplication = toApplication;
    }

    /**
     * Sets payload type.
     *
     * @param payloadTypeKey payload type key
     */
    public void setPayloadType(String payloadTypeKey) {
        this.payloadType = payloadTypeKey;
    }

    /**
     * Gets payload type.
     *
     * @return payload type key
     */
    public String getPayloadType() {
        return this.payloadType;
    }

    /**
     * Gets status.
     *
     * @return status (success, failure, unknown)
     */
    public String getStatus() {
        if (status == null) {
            return null;
        }
        return status.toString();
    }

    /**
     * Sets status.
     * Valid values are: "success", "failure" and "unknown".
     *
     * @param statusString status
     * @throws IllegalArgumentException when status is invalid
     */
    public void setStatus(String statusString) {
        Enum.Status status = Enum.Status.getStatus(statusString);

        if (status == null) {
            StringBuilder sb = new StringBuilder("AinoMediator status must me one of: ");
            sb.append(Arrays.toString(Enum.Status.values()));
            throw new InvalidAgentConfigException(sb.toString());
        }

        this.status = status;
    }

    /**
     * Gets list of {@link Id}s.
     *
     * @return ids
     */
    public List<Id> getIdList() {
        return idList;
    }

    /**
     * Sets 'to' or 'from' application, based on direction.
     *
     * @param direction direction
     * @param applicationKey application key
     */
    public void setApplication(Enum.ApplicationDirection direction, String applicationKey) {
        switch (direction) {
            case TO:
                this.setToApplication(applicationKey);
                break;
            case FROM:
                this.setFromApplication(applicationKey);
                break;
        }
    }

}