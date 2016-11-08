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

package io.aino.agents.wso2.mediator.serializer;

import java.util.List;

import javax.xml.namespace.QName;

import io.aino.agents.wso2.mediator.AinoMediator;
import static io.aino.agents.wso2.mediator.config.AinoMediatorConfigConstants.*;

import org.apache.axiom.om.OMElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.config.xml.MediatorPropertySerializer;
import org.apache.synapse.config.xml.SynapseXPathSerializer;
import org.apache.synapse.mediators.MediatorProperty;

import io.aino.agents.wso2.mediator.util.Id;

/**
 * Class used by WSO2 ESB to serialize mediator to XML element.
 */
public class AinoMediatorSerializer extends AbstractMediatorSerializer {

    private static final QName ATT_KEY_Q = new QName("key");
    private static final QName ATT_VALUE_Q = new QName("value");
    private static final QName ATT_EXPRESSION_Q = new QName("expression");

    private static final QName ATT_SEPARATOR_Q = new QName("separator");

    private static final String DEFAULT_APPLICATION_KEY = "esb";

    @Override
    public String getMediatorClassName() {
        return AinoMediator.class.getName();
    }

    @Override
    protected OMElement serializeSpecificMediator(Mediator mediator) {

        if(null == mediator) {
            throw new NullPointerException("Cannot serialize given mediator. It was passed as null.");
        }

        if(!(mediator instanceof AinoMediator)) {
            StringBuilder sb = new StringBuilder("Cannot serialize given mediator.");
            sb.append(" Its type should be: ").append(AinoMediator.class.getSimpleName());
            sb.append(" Its type is: ").append(mediator.getType());
            throw new ClassCastException(sb.toString());
        }

        AinoMediator ainoMediator = (AinoMediator) mediator;

        OMElement logElement = fac.createOMElement(ROOT_TAG_NAME, synNS);

        addStatusToElement(ainoMediator, logElement);
        addOperationToElement(ainoMediator, logElement);
        addMessageToElement(ainoMediator, logElement);
        addIdsToElement(ainoMediator, logElement);
        addFromApplicationToElement(ainoMediator, logElement);
        addToApplicationToElement(ainoMediator, logElement);
        addPayloadTypeToElement(ainoMediator, logElement);
        addPropertiesToElement(ainoMediator, logElement);
        addSeparatorToElement(ainoMediator, logElement);

        return logElement;
    }

    private void addPropertiesToElement(AinoMediator ainoMediator, OMElement logElement) {
        List<MediatorProperty> properties = ainoMediator.getProperties();

        if(CollectionUtils.isEmpty(properties)){
            return;
        }

        MediatorPropertySerializer.serializeMediatorProperties(logElement, properties);
    }

    private void addSeparatorToElement(AinoMediator ainoMediator, OMElement logElement) {
        String separatorValue = ainoMediator.getSeparator();

        if(isNullOrEqual(separatorValue, DEFAULT_SEPARATOR)){
            return;
        }

        logElement.addAttribute(ATT_SEPARATOR_Q.getLocalPart(), separatorValue, null);
    }

    private boolean isNullOrEqual(String observed, String reference){

        if(null == observed){
            return true;
        }

        return StringUtils.equals(observed, reference);
    }

    private void addToApplicationToElement(AinoMediator ainoMediator, OMElement logElement) {
        String toKey = ainoMediator.getToApplication();

        if(StringUtils.equals(DEFAULT_APPLICATION_KEY, toKey)) {
            return;
        }

        OMElement toElement = fac.createOMElement(TO_TAG_NAME, synNS);
        toElement.addAttribute(APPLICATION_KEY_ATT_NAME, toKey, null);

        logElement.addChild(toElement);
    }


    private void addFromApplicationToElement(AinoMediator ainoMediator, OMElement logElement) {
        String fromKey = ainoMediator.getFromApplication();

        if(StringUtils.equals(DEFAULT_APPLICATION_KEY, fromKey)) {
            return;
        }

        OMElement fromElement = fac.createOMElement(FROM_TAG_NAME, synNS);
        fromElement.addAttribute(APPLICATION_KEY_ATT_NAME, fromKey, null);


        logElement.addChild(fromElement);
    }

    private void addIdsToElement(AinoMediator ainoMediator, OMElement logElement) {
        List<Id> idsList = ainoMediator.getIdList();

        if(null == idsList){
            return;
        }

        for (Id id : idsList) {
            OMElement idsElement = fac.createOMElement(IDS_TAG_NAME, synNS);
            idsElement.addAttribute(ATT_TYPE_Q.getLocalPart(), id.getTypeKey(), null);
            SynapseXPathSerializer.serializeXPath(id.getXPath(), idsElement, ATT_EXPRESSION_Q.getLocalPart());
            logElement.addChild(idsElement);
        }
    }

    private void addMessageToElement(AinoMediator ainoMediator, OMElement logElement) {

        if(null == ainoMediator.getMessage()){
            return;
        }

        OMElement messageElement = fac.createOMElement(MESSAGE_TAG_NAME, synNS);
        messageElement.addAttribute(ATT_VALUE_Q.getLocalPart(), ainoMediator.getMessage(), null);
        logElement.addChild(messageElement);
    }

    private void addOperationToElement(AinoMediator ainoMediator, OMElement logElement) {
        String operation = ainoMediator.getOperation();

        if(null == operation){
            return;
        }

        OMElement operationElement = fac.createOMElement(OPERATION_TAG_NAME, synNS);
        operationElement.addAttribute(ATT_KEY_Q.getLocalPart(), operation, null);

        logElement.addChild(operationElement);
    }

    private void addPayloadTypeToElement(AinoMediator ainoMediator, OMElement logElement) {
        String payloadType = ainoMediator.getPayloadType();

        if(null == payloadType) {
            return;
        }

        OMElement payloadElement = fac.createOMElement(PAYLOAD_TAG_NAME, synNS);
        payloadElement.addAttribute(PAYLOAD_TYPE_ATT_NAME, payloadType, null);

        logElement.addChild(payloadElement);
    }

    private void addStatusToElement(AinoMediator ainoMediator, OMElement logElement) {
        String status = ainoMediator.getStatus();

        if(null == status){
            return;
        }

        logElement.addAttribute(STATUS_ATT_NAME, status, null);
    }
}
