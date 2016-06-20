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

import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_FLOW_ID_PROPERTY_NAME;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_FLOW_ID_PROPERTY_PATH;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_IDS_PROPERTY_PATH;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_OPERATION_NAME_PROPERTY_NAME;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_OPERATION_NAME_PROPERTY_PATH;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.AINO_TIMESTAMP_PROPERTY_PATH;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.OPERATION_TAG_NAME;

import java.lang.reflect.Method;
import java.text.MessageFormat;
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
import org.apache.synapse.mediators.builtin.LogMediator;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import io.aino.agents.core.Agent;
import io.aino.agents.core.Transaction;
import io.aino.agents.core.config.InvalidAgentConfigException;
import io.aino.agents.wso2.mediator.util.Enum;
import io.aino.agents.wso2.mediator.util.Id;
import io.aino.agents.wso2.mediator.util.IdPropertyBuilder;
import io.aino.agents.wso2.mediator.util.MediatorLocation;

/**
 * Aino.io WSO2 ESB mediator.
 */
public class AinoMediator extends AbstractMediator {

    public Agent ainoAgent;
    private final LogMediator logMediator;
    private Enum.LogLevel level = Enum.LogLevel.CUSTOM;
    private Enum.LogCategory category;

    private String separator;
    private String operation;
    private String message;
    private String esbServerName;
    private String fromApplication;
    private String toApplication;
    private String payloadType;
    private Enum.Status status;
    private String flowId;

    private MediatorProperty messageProperty;
    private MediatorProperty idsProperty;
    private MediatorProperty fromProperty;
    private MediatorProperty toProperty;
    private MediatorProperty statusProperty;
    private MediatorProperty payloadTypeProperty;

    private MediatorLocation mediatorLocation;

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
        this.logMediator = new LogMediator();

        initLogMediator();
    }

    private void initLogMediator() {
        try {
            populateAinoMediatorProperties();
            populateSynapseLogMediatorProperties();
        } catch (JaxenException e) {
            throw new InvalidAgentConfigException("Failed to initialize the AinoMediator at " + this.mediatorLocation);
        }
    }

    private void populateSynapseLogMediatorProperties() throws JaxenException {

        logMediator.addProperty(messageProperty);
        logMediator.addProperty(statusProperty);
        logMediator.addProperty(payloadTypeProperty);

        logMediator.addProperty(getMediatorProperty(OPERATION_TAG_NAME, null, AINO_OPERATION_NAME_PROPERTY_PATH));
        logMediator.addProperty(getMediatorProperty("flowId", null, AINO_FLOW_ID_PROPERTY_PATH));
        logMediator.addProperty(getMediatorProperty("artifactType", mediatorLocation.getArtifactType(), null));
        logMediator.addProperty(getMediatorProperty("artifactName", mediatorLocation.getArtifactName(), null));
        logMediator.addProperty(getMediatorProperty("lineNumber", Integer.toString(mediatorLocation.getLineNumber()), null));

        if (ainoAgent.isEnabled()) {
            logMediator.addProperty(getMediatorProperty("ainoTimestamp", null, AINO_TIMESTAMP_PROPERTY_PATH));
        }
    }

    private void populateAinoMediatorProperties() throws JaxenException {
        messageProperty = getMediatorProperty("message", "", null);
        fromProperty = getMediatorProperty("from", "", null);
        toProperty = getMediatorProperty("to", "", null);
        statusProperty = getMediatorProperty("status", "", null);
        payloadTypeProperty = getMediatorProperty("payloadType", "", null);
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

        if(null != this.idsProperty){
            return;
        }

        try {
            idsProperty = getMediatorProperty("ids", null, AINO_IDS_PROPERTY_PATH);
            logMediator.addProperty(idsProperty);
        } catch (JaxenException e) {
            throw new InvalidAgentConfigException("Failed to initialize the AinoMediator at " + mediatorLocation);
        }
    }

    /**
     * Adds custom list of {@link MediatorProperty}.
     * They will be logged by ESB.
     *
     * @param properties properties to add
     */
    public void setProperties(List<MediatorProperty> properties) {
        customProperties = properties;
        logMediator.addAllProperties(properties);
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

        if (idList.isEmpty()) {
            logMediator.getProperties().remove(idsProperty);
        }

        initTransportHeadersMap(context);

        validateOrSetAinoFlowId(context);
        validateOrSetAinoOperationName(context);

        Transaction transaction = createTransaction(context);
        new IdPropertyBuilder(this.idList).buildToContext(context, transaction);

        logMediator.mediate(context);

        processTransaction(context, transaction);

        return true;
    }

    private void processTransaction(MessageContext context, Transaction transaction) {
        if(transaction == null) { return; }
        addFieldsToTransaction(transaction);

        List<MediatorProperty> properties = logMediator.getProperties();
        if(properties == null) { return; }

        for (MediatorProperty property : properties) {
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
        transaction.setMessage(this.message);
        transaction.setOperationKey(this.operation);
        transaction.setStatus(this.status == null ? "" : this.status.toString());
        transaction.setFlowId(this.flowId);
        transaction.setPayloadTypeKey(this.payloadType);
    }

    private Transaction createTransaction(MessageContext context) {
        Transaction transaction = null;
        if (ainoAgent.isEnabled()) {
            transaction = ainoAgent.newTransaction();

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
    private void initTransportHeadersMap(MessageContext context) {
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
        Object headers = axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headers == null) {
            Map<String, String> headersMap = new HashMap<String, String>();
            context.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
        }
    }

    private String validateOrSetAinoOperationName(MessageContext context) {

        String ainoOperationName = getOperationNameFromTransportHeaders(context);

        if (null == ainoOperationName) {
            ainoOperationName = getOperationNameFromMessageContext(context);
        }

        if(null != ainoOperationName){
            return ainoOperationName;
        }

        if(null == operation){
            log.warn("The AinoMediator encountered an operation it isn't registered for. Undefined operation.");
            return null;
        }

        return getOperationNameFromMediator(context);

    }

    private String getOperationNameFromMediator(MessageContext context) {
        Map<String, String> headersMap = getTransportHeadersMap(context);
        headersMap.put(AINO_OPERATION_NAME_PROPERTY_NAME, operation);
        return operation;
    }

    private String getOperationNameFromTransportHeaders(MessageContext context){
        // This logic is in place for situations where the message is coming back from a system which doesn't return custom transport headers.
        try {
            Map<String, String> headersMap = getTransportHeadersMap(context);
            return headersMap.get(AINO_OPERATION_NAME_PROPERTY_NAME);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private String getOperationNameFromMessageContext(MessageContext context){
        try {
            String name = (String) context.getProperty(AINO_OPERATION_NAME_PROPERTY_NAME);
            Map<String, String> headersMap = getTransportHeadersMap(context);
            headersMap.put(AINO_OPERATION_NAME_PROPERTY_NAME, name);
            return name;
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getTransportHeadersMap(MessageContext context) {
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context).getAxis2MessageContext();
        return (Map<String, String>) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
    }


    private void validateOrSetAinoFlowId(MessageContext context) {
        Map<String, String> headersMap = getTransportHeadersMap(context);

        String flowId = headersMap.get(AINO_FLOW_ID_PROPERTY_NAME);

        if(null == flowId){
            flowId = getFlowIdFromMessageContext(context);
        }

        if(null == flowId){
            flowId = ((Axis2MessageContext) context).getAxis2MessageContext().getMessageID();
        }

        this.flowId = flowId;
        headersMap.put(AINO_FLOW_ID_PROPERTY_NAME, this.flowId);
    }

    private String getFlowIdFromMessageContext(MessageContext context) {
        try {
            String flowId = (String) context.getProperty(AINO_FLOW_ID_PROPERTY_NAME);
            Map<String, String> headersMap = getTransportHeadersMap(context);
            headersMap.put(AINO_FLOW_ID_PROPERTY_NAME, flowId);
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
     * Sets mediator location.
     *
     * @param mediatorLocation mediator location
     */
    public void setMediatorLocation(MediatorLocation mediatorLocation) {
        this.mediatorLocation = mediatorLocation;
    }

    /**
     * Gets logging category.
     *
     * @return logging category
     */
    public String getCategory() {
        return category == null ? null : category.toString();
    }

    /**
     * Sets logging category.
     *
     * @param categoryString category as string
     * @throws IllegalArgumentException when logging category is invalid
     */
    public void setCategory(String categoryString) {
        Enum.LogCategory category;
        if (categoryString == null || categoryString.isEmpty()) {
            category = Enum.LogCategory.INFO;
        } else {
            category = Enum.LogCategory.getLogCategory(categoryString);
        }

        if (category == null) {
            throw new IllegalArgumentException(MessageFormat.format("AinoLogMediatorCategory must me one of: {0}",
                    Arrays.toString(Enum.LogCategory.values())));
        }

        this.category = category;

        logMediator.setCategory(category.getCategoryInt());
    }

    /**
     * Gets logging level.
     *
     * @return log level
     */
    public String getLevel() {
        return level.toString();
    }

    /**
     * Sets logging level.
     *
     * @param levelString log level
     * @throws IllegalArgumentException when log level is not valid
     */
    public void setLevel(String levelString) {
        Enum.LogLevel level = parseLogLevel(levelString);
        assertValidLogLevel(level);

        this.level = level;
        logMediator.setLogLevel(level.getLevelInt());
    }

    private void assertValidLogLevel(Enum.LogLevel level) {
        if (level == null) {
            StringBuilder sb = new StringBuilder("AinoMediator level must me one of: ");
            sb.append(Arrays.toString(Enum.LogLevel.values()));
            throw new IllegalArgumentException(sb.toString());
        }
    }

    private Enum.LogLevel parseLogLevel(String levelString) {
        if (StringUtils.isEmpty(levelString)) {
            return Enum.LogLevel.CUSTOM;
        }
        return Enum.LogLevel.getLogLevel(levelString);
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
        logMediator.setSeparator(separator);
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

        messageProperty.setValue(message);
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

        fromProperty.setValue(ainoAgent.getAgentConfig().getApplications().getEntry(fromApplication));
        logMediator.addProperty(fromProperty);
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

        toProperty.setValue(ainoAgent.getAgentConfig().getApplications().getEntry(toApplication));
        logMediator.addProperty(toProperty);
    }

    /**
     * Sets payload type.
     *
     * @param payloadTypeKey payload type key
     */
    public void setPayloadType(String payloadTypeKey) {
        this.payloadType = payloadTypeKey;

        payloadTypeProperty.setValue(ainoAgent.getAgentConfig().getPayloadTypes().getEntry(payloadTypeKey));
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
        if (StringUtils.isEmpty(statusString)) {
            logMediator.getProperties().remove(statusProperty);
            return;
        }

        Enum.Status status = Enum.Status.getStatus(statusString);

        if (status == null) {
            StringBuilder sb = new StringBuilder("AinoMediator status must me one of: ");
            sb.append(Arrays.toString(Enum.Status.values()));
            throw new IllegalArgumentException(sb.toString());
        }

        this.status = status;

        statusProperty.setValue(statusString);
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