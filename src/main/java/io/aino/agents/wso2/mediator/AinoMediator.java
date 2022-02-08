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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;

/**
 * Aino.io WSO2 ESB mediator.
 */
public class AinoMediator extends AbstractMediator {
    public static String UNKNOWN_DYNAMIC_APPLICATION = "UnKnown_App";
    public static String UNKNOWN_DYNAMIC_OPERATION = "UnKnown_Operation";
    public static String UNKNOWN_DYNAMIC_PAYLOADTYPE = "UnKnown_Payload";
    public static String UNKNOWN_DYNAMIC_IDTYPE = "UnKnown_IDType";

    public Agent ainoAgent;

    private String separator;
    private String operation;
    private SynapseXPath dynamicOperation = null;
    private String message;
    private SynapseXPath dynamicMessage = null;
    private String esbServerName;
    private String fromApplication;
    private SynapseXPath dynamicFromApplication = null;
    private String toApplication;
    private SynapseXPath dynamicToApplication = null;
    private String payloadType;
    private SynapseXPath dynamicPayloadType = null;
    private Enum.Status status;
    private SynapseXPath dynamicStatus = null;
    private String multiids;
    private SynapseXPath dynamicMultiids = null;

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
            "multiids",
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
            processMultiids(context, transaction);
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


    private void processMultiids(MessageContext context, Transaction transaction) {
        String multiidsValues = null;    
        // If PayloadType is given as dynamic expression. Process it here. 
        if (this.getDynamicMultiids() != null) {
            multiidsValues = processDynamicMultiids(context);
        } else {
            // Static value was used 
            multiidsValues = this.multiids;
        } 

        if (multiidsValues != null) {
            //Sample whole multiidsValues: some_other_id=value1,value2||some_other_id2=xxx 
            String[] multiids = multiidsValues.split("\\|\\|");
            // Adfter split first item in list is : some_other_id=value1,value2
            for (String idTypeAndValues : multiids) {
                String[] idTypeAndValuesArray = idTypeAndValues.split("=");
                // Split to two parts the type: some_other_id and the value:value1,value2
                if (idTypeAndValuesArray.length == 2) {
                    String idType = idTypeAndValuesArray[0];
                    if (!ainoAgent.getAgentConfig().getIdTypes().entryExists(idType)) {
                        StringBuilder sb = new StringBuilder("An invalid id type key has been given to a AinoMediator ");
                        sb.append(idType).append(" element, using Unknown as id type");
                        log.warn(sb.toString());
                        idType = UNKNOWN_DYNAMIC_IDTYPE;
                        if (!ainoAgent.getAgentConfig().getIdTypes().entryExists(idType)) { 
                            // Add the unknown id type to the list of id types, if it is not already there.
                            ainoAgent.getAgentConfig().getIdTypes().addEntry(idType, idType);
                        } 
                    }

                    String idValues = idTypeAndValuesArray[1];
                    String[] idValuesArray = idValues.split(",");
                    // If there are multiple values separated by comma (value1,value2), put those to separate array
                    List<String> idValuesAsArray = new ArrayList<String>();  
                    for (String idValue : idValuesArray) {
                        idValuesAsArray.add(idValue);
                    }
            
                    // Add the some_other_id and its values (value1,value2) to transaction
                    transaction.addIdsByTypeKey(idType, idValuesAsArray);
                }
            }
        }
    }

    private void processTransaction(MessageContext context, Transaction transaction) {
        if(transaction == null) { return; }

        // Dynamic operation handling. If Dynamic operation is given. it will override any other style of giving the operation name           
        // The static value of operation handlig is doen prio of this by validateOrSetAinoOperationName. So we only check do we need to override it 
        if (this.getDynamicOperation() != null) {
            transaction.setOperationKey(processDynamicOperation(context));
        }  

        // If PayloadType is given as dynamic expression. Process it here. 
        if (this.getDynamicPayloadType() != null) {
            transaction.setPayloadTypeKey(processDynamicPayloadType(context));
        } else {
            // Static value was used 
            transaction.setPayloadTypeKey(this.payloadType);
        } 

        // status atribute handling moved to here since it can be dynamically defined          
        if (this.getDynamicStatus() != null) {
            transaction.setStatus(processDynamicStatus(context));
        } else {
            // Static status attribute was used 
            transaction.setStatus(this.status == null ? "" : this.status.toString());
        }

        // If message is given as dynamic expression. Process it here. 
        if (this.getDynamicMessage() != null) {
            transaction.setMessage(processDynamicMessage(context));
        } else {
            // Static value attribute was used 
            transaction.setMessage(this.message);
        }

        // From and to applications movoved to here since those can be dynamically defined          
        if (this.getDynamicFromApplication() != null) {
            transaction.setFromKey(processDynamicApplication(Enum.ApplicationDirection.FROM, context));
        } else {
            // Static value attribute was used 
            transaction.setFromKey(this.fromApplication);
        }
        if (this.getDynamicToApplication() != null) {
            transaction.setToKey(processDynamicApplication(Enum.ApplicationDirection.TO, context));
        } else {
            // Static value attribute was used 
            transaction.setToKey(this.toApplication);
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

    public void setDynamicOperation(SynapseXPath xpath){
        this.dynamicOperation = xpath;
    }

    public  SynapseXPath getDynamicOperation(){
        return this.dynamicOperation;
    }

    protected String processDynamicOperation(MessageContext context){
        SynapseXPath expression = this.dynamicOperation;
        String operationKey = null;
        Boolean operationKeyExist = false;
        if (expression != null) {
            try {
                Object evaluationResult = expression.evaluate(context);
                if (evaluationResult != null){
                    operationKey = getExpressionValue(evaluationResult);
                    if (ainoAgent.operationExists(operationKey)) {
                        operationKeyExist = true;
                        return operationKey;
                    }                    
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic operation  ");
                sb.append(" XPath expression: ").append(expression.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
            if (operationKeyExist == false){
                // The dynamic operation name is NOT in the configs OR the Xpath was corrupted. 
                // So lets use UnKnown operation name
                // NOTE we add the UnKnown application name dynamically if it does not yet exist.                          
                String origOperationKey = operationKey;
                operationKey = UNKNOWN_DYNAMIC_OPERATION;
                if (!ainoAgent.operationExists(operationKey)) {
                    ainoAgent.getAgentConfig().getOperations().addEntry(UNKNOWN_DYNAMIC_OPERATION, UNKNOWN_DYNAMIC_OPERATION);
                }   
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic operation ");
                sb.append(" using XPath expression: ").append(expression.toString());
                sb.append(" Exception message: operation does not exist in config, name of operation: ").append(origOperationKey).append(" Doing fallback and using UnKnown as operation name");
                log.warn(sb.toString());                                     
            }
        }
        return operationKey;
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
                    return getExpressionValue(evaluationResult);
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
  
      /** If From is given as expression then the expression value it is set to this variable. 
     * And actual referenced value is calculated on processDynamicApplication when needed 
     * 
     * Example   
     *    <property name="myValue" value="theApplicationName"/>
     *    <ainoLog status="failure">
     *       <operation key="myOper"/>
     *       <message value="MyMessage"/>
     *       <from expression="//koe"/>
     *       <to expression="$ctx:myValue"/>
     *       <payloadType key="delivery"/>
     *    </ainoLog>
     * */
    public void setDynamicFromApplication(SynapseXPath xpath){
        this.dynamicFromApplication= xpath;
    }

    /**
     *  Returns the expression of from 
     * @return
     */
    public  SynapseXPath getDynamicFromApplication(){
        return this.dynamicFromApplication;
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

    /** If To is given as expression then the expression value it is set to this variable. 
     * And actual referenced value is calculated on processDynamicApplication when needed 
     * 
     * Example   
     *    <property name="myValue" value="theApplicationName"/>
     *    <ainoLog status="failure">
     *       <operation key="myOper"/>
     *       <message value="MyMessage"/>
     *       <from expression="//koe"/>
     *       <to expression="$ctx:myValue"/>
     *       <payloadType key="delivery"/>
     *    </ainoLog>
     * */
    public void setDynamicToApplication(SynapseXPath xpath){
        this.dynamicToApplication= xpath;
    }

    /**
     *  Returns the expression of from 
     * @return
     */
    public  SynapseXPath getDynamicToApplication(){
        return this.dynamicToApplication;
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

    /**
     *  Process 'to' or 'from' application based on direction 
     * This will calculated the outcome of the expression value set for the 'to' or 'from'  
     * @param direction
     * @param context
     * @return The calculated value based on the messagecontext and expression.  OR null if value is not found
     */
    protected String processDynamicApplication(Enum.ApplicationDirection direction, MessageContext context){
        SynapseXPath expression = null;
        switch (direction) {
            case TO:
                expression =  this.getDynamicToApplication();
                break;
            case FROM:
                expression =  this.getDynamicFromApplication();
                break;
        }
        String applicationKey = null;
        Boolean applicationKeyExist = false;
        if (expression != null) {
            try {
                Object evaluationResult = expression.evaluate(context);
                if (evaluationResult != null){
                    applicationKey = getExpressionValue(evaluationResult);
                    if (ainoAgent.applicationExists(applicationKey)) {
                        applicationKeyExist = true;
                        return applicationKey;
                    }                    
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic Application of direction ");
                sb.append(direction.toString());
                sb.append(" XPath expression: ").append(expression.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
            if (applicationKeyExist == false){
                // The dynamic application name is NOT in the configs OR the Xpath was corrupted. 
                // So lets use UnKnown application name
                // NOTE we add the UnKnown application name dynamically if it does not yet exist.                          
                String origApplicationKey = applicationKey;
                applicationKey = UNKNOWN_DYNAMIC_APPLICATION;
                if (!ainoAgent.applicationExists(applicationKey)) {
                    ainoAgent.getAgentConfig().getApplications().addEntry(UNKNOWN_DYNAMIC_APPLICATION, UNKNOWN_DYNAMIC_APPLICATION);
                }   
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic Application of direction ");
                sb.append(direction.toString());
                sb.append(" using XPath expression: ").append(expression.toString());
                sb.append(" Exception message: application does not exist in config, name of application: ").append(origApplicationKey).append(" Doing fallback and using UnKnown as application name");
                log.warn(sb.toString());                                     
            }
        }
        return applicationKey;
    }

    /**
     * Set expression value of dynamic 'to' or 'from' based on direction  
     * @param direction
     * @param xpath
     */
    public void setDynamicApplication(Enum.ApplicationDirection direction, SynapseXPath xpath) {
        switch (direction) {
            case TO:
                this.setDynamicToApplication(xpath);
                break;
            case FROM:
                this.setDynamicFromApplication(xpath);
                break;
        }
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

    public  SynapseXPath getDynamicPayloadType(){
        return this.dynamicPayloadType;
    }

    public void setDynamicPayloadType(SynapseXPath xpath){
        this.dynamicPayloadType = xpath;
    }


    protected String processDynamicPayloadType(MessageContext context){
        SynapseXPath expression = this.dynamicPayloadType;
        String payloadTypeKey = null;
        Boolean payloadTypeKeyExist = false;
        if (expression != null) {
            try {
                Object evaluationResult = expression.evaluate(context);
                if (evaluationResult != null){
                    payloadTypeKey = getExpressionValue(evaluationResult);
                    if (ainoAgent.payloadTypeExists(payloadTypeKey)) {
                        payloadTypeKeyExist = true;
                        return payloadTypeKey;
                    }                    
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic payloadType  ");
                sb.append(" XPath expression: ").append(expression.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
            if (payloadTypeKeyExist == false){
                // The dynamic payloadType name is NOT in the configs OR the Xpath was corrupted. 
                // So lets use UnKnown payloadType name
                // NOTE we add the UnKnown payloadType name dynamically if it does not yet exist.                          
                String origPayloadTypeKey = payloadType;
                payloadType = UNKNOWN_DYNAMIC_PAYLOADTYPE;
                if (!ainoAgent.payloadTypeExists(payloadTypeKey)) {
                    ainoAgent.getAgentConfig().getPayloadTypes().addEntry(payloadType, payloadType);
                }   
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic payloadType ");
                sb.append(" using XPath expression: ").append(expression.toString());
                sb.append(" Exception message: payloadType does not exist in config, name of payloadType: ").append(origPayloadTypeKey).append(" Doing fallback and using UnKnown as payloadType name");
                log.warn(sb.toString());                                     
            }
        }
        return payloadTypeKey;
    }

    public String getMultiids() {
        return this.multiids;
    }
    public void setMultiids(String multiidsValue) {
        this.multiids = multiidsValue;
    }


    public  SynapseXPath getDynamicMultiids(){
        return this.dynamicMultiids;
    }

    public void setDynamicMultiids(SynapseXPath xpath){
        this.dynamicMultiids = xpath;
    }


    protected String processDynamicMultiids(MessageContext context){
        SynapseXPath expression = this.dynamicMultiids;
        String multiidsKey = null;
        Boolean multiidsKeyExist = false;
        if (expression != null) {
            try {
                Object evaluationResult = expression.evaluate(context);
                if (evaluationResult != null){
                    multiidsKey = getExpressionValue(evaluationResult);
                    multiidsKeyExist = true;
//                    if (ainoAgent.MultiidsExists(payloadTypeKey)) {
//                        payloadTypeKeyExist = true;
//                        return payloadType;
//                    }                    
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic Multiids  ");
                sb.append(" XPath expression: ").append(expression.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
            // if (multiidsKeyExist == false){
            //     // The dynamic payloadType name is NOT in the configs OR the Xpath was corrupted. 
            //     // So lets use UnKnown payloadType name
            //     // NOTE we add the UnKnown payloadType name dynamically if it does not yet exist.                          
            //     String origMultiidsKey = multiidsKey;
            //     payloadType = UNKNOWN_DYNAMIC_PAYLOADTYPE;
            //     if (!ainoAgent.payloadTypeExists(payloadTypeKey)) {
            //         ainoAgent.getAgentConfig().getPayloadTypes().addEntry(payloadType, payloadType);
            //     }   
            //     StringBuilder sb = new StringBuilder("Error while resolving the dynamic payloadType ");
            //     sb.append(" using XPath expression: ").append(expression.toString());
            //     sb.append(" Exception message: payloadType does not exist in config, name of payloadType: ").append(origPayloadTypeKey).append(" Doing fallback and using UnKnown as payloadType name");
            //     log.warn(sb.toString());                                     
            // }
        }
        return multiidsKey;
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

    /** If Status is given as expression then the expression value it is set to this variable. 
     * And actual referenced value is calculated on processDynamicApplication when needed 
     *  NOTE the actual values of status still must be success, failure OR unknonw 
     * Example   
     *    <property name="myValue" value="failure"/>
     *    <ainoLog statusExpression="$ctx:myValue">
     *     .... 
     * 
     * Compared to the static status
     *    <ainoLog status="failure">
     *      ..... 
     * */
    public void setDynamicStatus(SynapseXPath xpath){
        this.dynamicStatus= xpath;
    }

    /**
     *  Returns the expression of from 
     * @return
     */
    public  SynapseXPath getDynamicStatus(){
        return this.dynamicStatus;
    }

    protected String processDynamicStatus(MessageContext context){
        SynapseXPath expression = this.getDynamicStatus();
        String calculatedStatus = null;
        Enum.Status status = null;
        if (expression != null) {
            try {
                Object evaluationResult = expression.evaluate(context);
                if (evaluationResult != null){
                    calculatedStatus = getExpressionValue(evaluationResult);
                    status = Enum.Status.getStatus(calculatedStatus);
                }
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic Status  ");
                sb.append(" XPath expression: ").append(expression.toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
            if (status == null){
                // The dynamic status is NOT valid status OR the Xpath was corrupted. 
                // So lets use UnKnown as status 
                status = Enum.Status.UNKNOWN;
                StringBuilder sb = new StringBuilder("Error while resolving the dynamic Status ");
                sb.append(" using XPath expression: ").append(expression.toString());
                sb.append(" Exception message: Can not calculate valid status from given value: ").append(calculatedStatus).append(" Doing fallback and using unknown as status");
                log.warn(sb.toString());                                     
            }
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
     *  Return the data of evaluated, NOTE if the Xpath finds several values. This returns ONLY the first found value 
     * @param evaluationResult
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getExpressionValue(Object evaluationResult) {
        List<String> transactionIdList = new ArrayList<String>();
        if (evaluationResult instanceof List) {
            List<String> entryList = getValuesFromList((List<Object>) evaluationResult);
            transactionIdList.addAll(entryList);
        } else {
            transactionIdList.add(String.valueOf(evaluationResult));
        }
        if (transactionIdList.size() > 0){
            return transactionIdList.get(0);
        }
        return null;
    }

    private List<String> getValuesFromList(List<Object> results) {
        List<String> transactionIdList = new ArrayList<String>();
        for (Object result : results) {
            String idString = getSingleValue(result);
            if (StringUtils.isNotEmpty(idString)) {
                transactionIdList.add(idString);
            }
        }
        return transactionIdList;
    }

    private String getSingleValue(Object result) {
        if (result instanceof OMElement) {
            return ((OMElement) result).getText();
        } else if (result instanceof OMAttribute) {
            return ((OMAttribute) result).getAttributeValue();
        } else if (result instanceof OMText) {
            return ((OMText) result).getText();
        }
        return null;
    }
}