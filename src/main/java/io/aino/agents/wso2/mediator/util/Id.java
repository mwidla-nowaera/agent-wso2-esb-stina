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

import org.apache.synapse.util.xpath.SynapseXPath;

/**
 * Class for IDs.
 * Ids are {@link SynapseXPath}, which is used to get the Ids from the message.
 */
public class Id {


    private final String typeKey;
    private final SynapseXPath xPath;

    /**
     * Constructor.
     *
     * @param typeKey Id type key
     * @param xPath xpath of the ids
     */
    public Id(String typeKey, SynapseXPath xPath) {
        this.typeKey = typeKey;
        this.xPath = xPath;
    }

    /**
     * Returns the type key of this Id.
     *
     * @return type key
     */
    public String getTypeKey() {
        return typeKey;
    }

    /**
     * Returns the xpath of Ids.
     *
     * @return xpath to Ids
     */
    public SynapseXPath getXPath() {
        return xPath;
    }

}
