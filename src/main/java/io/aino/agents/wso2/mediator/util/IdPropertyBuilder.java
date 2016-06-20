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

package io.aino.agents.wso2.mediator.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.jaxen.JaxenException;

import io.aino.agents.core.Transaction;

/**
 * Class for evaluation list of {@link Id}s against message context.
 * Adds extracted Ids to {@link Transaction}.
 */
public class IdPropertyBuilder {
    private static final Log log = LogFactory.getLog(IdPropertyBuilder.class);

    private final List<Id> idList;

    /**
     * Constructor.
     *
     * @param idList List of ids
     */
    public IdPropertyBuilder(List<Id> idList){
        this.idList = idList;
    }

    /**
     * Evaluates message context and adds extracted ids to transaction.
     *
     * @param context message context to evaluate
     * @param transaction log entry to add the extracted ids
     */
    public void buildToContext(MessageContext context, Transaction transaction) {
        if (CollectionUtils.isEmpty(this.idList) || null == context || null == transaction){
            return;
        }

        populateTransactionIds(context, transaction);
    }

    private void populateTransactionIds(MessageContext context, Transaction transaction) {
        for (Id id : this.idList) {
            try {
                Object evaluationResult = id.getXPath().evaluate(context);
                List<String> transactionIdList = getTransactionIdList(evaluationResult);
                transaction.addIdsByTypeKey(id.getTypeKey(), transactionIdList);
            } catch (JaxenException e) {
                StringBuilder sb = new StringBuilder("Error while resolving the ID");
                sb.append(" XPath expression: ").append(id.getXPath().toString());
                sb.append(" Exception message: ").append(e.getMessage());
                log.warn(sb.toString(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getTransactionIdList(Object evaluationResult) {
        List<String> transactionIdList = new ArrayList<String>();
        if (evaluationResult instanceof List) {
            List<String> entryList = getIdList((List<Object>) evaluationResult);
            transactionIdList.addAll(entryList);
        } else {
            transactionIdList.add(String.valueOf(evaluationResult));
        }
        return transactionIdList;
    }

    private List<String> getIdList(List<Object> results) {
        List<String> transactionIdList = new ArrayList<String>();
        for (Object result : results) {
            String idString = getIdString(result);
            if (StringUtils.isNotEmpty(idString)) {
                transactionIdList.add(idString);
            }
        }
        return transactionIdList;
    }

    private String getIdString(Object result) {
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
