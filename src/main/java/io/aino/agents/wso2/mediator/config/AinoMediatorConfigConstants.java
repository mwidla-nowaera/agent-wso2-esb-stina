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

package io.aino.agents.wso2.mediator.config;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.synapse.config.xml.XMLConfigConstants;

/**
 * Class containing constants.
 * Constants include mainly XML element names and QNames.
 */
public class AinoMediatorConfigConstants {

    public static class Deprecated {
        @java.lang.Deprecated
        public static final String APP_SPECIFIER_KEY_ATT_NAME = "specifierKey";

        @java.lang.Deprecated
        public static final QName ATT_APP_SPECIFIER_KEY_Q = new QName(APP_SPECIFIER_KEY_ATT_NAME);
    }

    public static final String ESB_DIR = (System.getenv("CARBON_HOME") != null ? System.getenv("CARBON_HOME") : System
            .getProperty("user.dir"));

    public static final String ESB_CONFIG_DIR = ESB_DIR + "/repository/conf/";

    public static final String AXIS2_CONFIG_FILE_PATH = ESB_CONFIG_DIR + "axis2/axis2.xml";
    public static final String AINO_CONFIG_FILE_NAME = "ainoLogMediatorConfig.xml";
    public static final String AINO_CONFIG_FILE_PATH = ESB_CONFIG_DIR + AINO_CONFIG_FILE_NAME;

    public static final String DEFAULT_CATEGORY = "INFO";
    public static final String DEFAULT_LEVEL = "custom";
    public static final String DEFAULT_SEPARATOR = ",";

    public static final String NAMESPACE_STRING = XMLConfigConstants.SYNAPSE_NAMESPACE;

    public static final String CONFIG_AINO_LOGER_SERVICE_TAG_NAME = "ainoLoggerService";
    public static final String CONFIG_ADDRESS_TAG_NAME = "address";
    public static final String CONFIG_SEND_TAG_NAME = "send";

    public static final String CONFIG_ENABLED_ATT_NAME = "enabled";
    public static final String CONFIG_URI_ATT_NAME = "uri";
    public static final String CONFIG_APIKEY_ATT_NAME = "apiKey";
    public static final String CONFIG_INTERVAL_ATT_NAME = "interval";
    public static final String CONFIG_SIZE_THRESHOLD_ATT_NAME = "sizeThreshold";

    public static final String LOG_MEDIATOR_TAG_NAME = "log";

    public static final String ROOT_TAG_NAME = "ainoLog";

    public static final String STATUS_ATT_NAME = "status";

    public static final String APPLICATION_KEY_ATT_NAME = "applicationKey";
    public static final String PAYLOAD_TYPE_ATT_NAME = "key";

    public static final String TYPE_ATT_NAME = "typeKey";

    public static final String MESSAGE_TAG_NAME = "message";
    public static final String OPERATION_TAG_NAME = "operation";
    public static final String IDS_TAG_NAME = "ids";
    public static final String FROM_TAG_NAME = "from";
    public static final String TO_TAG_NAME = "to";
    public static final String PAYLOAD_TAG_NAME = "payloadType";


    public static final QName ATT_CATEGORY_Q = new QName("category");
    public static final QName ATT_LEVEL_Q = new QName("level");
    public static final QName ATT_SEPARATOR_Q = new QName("separator");

    public static final QName CONFIG_AINO_LOGGER_SERVICE_Q = new QName(CONFIG_AINO_LOGER_SERVICE_TAG_NAME);
    public static final QName CONFIG_ADDRESS_Q = new QName(CONFIG_ADDRESS_TAG_NAME);
    public static final QName CONFIG_SEND_Q = new QName(CONFIG_SEND_TAG_NAME);

    public static final QName CONFIG_ENABLED_ATT_Q = new QName(CONFIG_ENABLED_ATT_NAME);
    public static final QName CONFIG_URI_ATT_Q = new QName(CONFIG_URI_ATT_NAME);
    public static final QName CONFIG_APIKEY_ATT_Q = new QName(CONFIG_APIKEY_ATT_NAME);
    public static final QName CONFIG_INTERVAL_ATT_Q = new QName(CONFIG_INTERVAL_ATT_NAME);
    public static final QName CONFIG_SIZE_THRESHOLD_ATT_Q = new QName(CONFIG_SIZE_THRESHOLD_ATT_NAME);

    public static final QName LOG_MEDIATOR_ROOT_Q = new QName(NAMESPACE_STRING, LOG_MEDIATOR_TAG_NAME);

    public static final QName ROOT_TAG = new QName(NAMESPACE_STRING, ROOT_TAG_NAME, XMLConstants.DEFAULT_NS_PREFIX);

    public static final QName ATT_STATUS_Q = new QName(STATUS_ATT_NAME);

    public static final QName ATT_APPLICATION_KEY_Q = new QName(APPLICATION_KEY_ATT_NAME);
    public static final QName ATT_PAYLOAD_TYPE_KEY_Q = new QName(PAYLOAD_TYPE_ATT_NAME);

    public static final QName ATT_TYPE_Q = new QName(TYPE_ATT_NAME);

    public static final QName MESSAGE_Q = new QName(NAMESPACE_STRING, MESSAGE_TAG_NAME, XMLConstants.DEFAULT_NS_PREFIX);
    public static final QName OPERATION_Q = new QName(NAMESPACE_STRING, OPERATION_TAG_NAME,
            XMLConstants.DEFAULT_NS_PREFIX);
    public static final QName IDS_Q = new QName(NAMESPACE_STRING, IDS_TAG_NAME);
    public static final QName FROM_Q = new QName(NAMESPACE_STRING, FROM_TAG_NAME);
    public static final QName TO_Q = new QName(NAMESPACE_STRING, TO_TAG_NAME);
    public static final QName PAYLOAD_Q = new QName(NAMESPACE_STRING, PAYLOAD_TAG_NAME);

    public static final String AINO_OPERATION_NAME_PROPERTY_NAME = "ainoOperationName";
    public static final String AINO_IDS_PROPERTY_NAME = "ainoIds";
    public static final String AINO_FLOW_ID_PROPERTY_NAME = "ainoFlowId";
    public static final String AINO_TIMESTAMP_PROPERTY_NAME = "ainoTimestamp";
    public static final String AINO_ARTIFACT_TYPE_PROPERTY_NAME = "ainoArtifactType";
    public static final String AINO_ARTIFACT_NAME_PROPERTY_NAME = "ainoArtifactName";
    public static final String AINO_ARTIFACT_LINE_NUMBER_PROPERTY_NAME = "ainoArtifactLineNumber";

    public static final String SYNAPSE_MESSAGE_CONTEXT_PREFIX = "$ctx:";
    public static final String AXIS_TRANSPORT_HEADER_PREFIX = "$trp:";

    public static final String AINO_OPERATION_NAME_PROPERTY_PATH = AXIS_TRANSPORT_HEADER_PREFIX
            + AINO_OPERATION_NAME_PROPERTY_NAME;
    public static final String AINO_IDS_PROPERTY_PATH = SYNAPSE_MESSAGE_CONTEXT_PREFIX + AINO_IDS_PROPERTY_NAME;
    public static final String AINO_FLOW_ID_PROPERTY_PATH = AXIS_TRANSPORT_HEADER_PREFIX
            + AINO_FLOW_ID_PROPERTY_NAME;
    public static final String AINO_TIMESTAMP_PROPERTY_PATH = SYNAPSE_MESSAGE_CONTEXT_PREFIX
            + AINO_TIMESTAMP_PROPERTY_NAME;
    public static final String AINO_ARTIFACT_TYPE_PROPERTY_PATH = SYNAPSE_MESSAGE_CONTEXT_PREFIX
            + AINO_ARTIFACT_TYPE_PROPERTY_NAME;
    public static final String AINO_ARTIFACT_NAME_PROPERTY_PATH = SYNAPSE_MESSAGE_CONTEXT_PREFIX
            + AINO_ARTIFACT_NAME_PROPERTY_NAME;
    public static final String AINO_ARTIFACT_LINE_NUMBER_PROPERTY_PATH = SYNAPSE_MESSAGE_CONTEXT_PREFIX
            + AINO_ARTIFACT_LINE_NUMBER_PROPERTY_NAME;
}
