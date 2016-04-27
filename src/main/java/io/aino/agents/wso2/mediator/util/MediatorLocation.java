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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.xml.XMLConfigConstants;

/**
 * Utility class for logging.
 * Used to get ainoLog mediator location from configuration files.
 */
public class MediatorLocation {

    private static final Set<QName> ANCESTOR_QNAMES = getAncestorQNameSet();
    private static final QName ATT_NAME = new QName("", "name");
    private ArtifactType artifactType;
    private String artifactName;
    private int lineNumber;

    private MediatorLocation() {}

    /**
     * Returns mediator location based on mediator element.
     *
     * @param mediatorElement element to find the location for
     * @return mediator location
     */
    public static MediatorLocation getMediatorLocation(OMElement mediatorElement) {
        MediatorLocation ml = new MediatorLocation();
        OMElement artifactElement = getFirstAncestorByName(mediatorElement, ANCESTOR_QNAMES);
        ml.artifactType = ArtifactType.getArtifactType(artifactElement.getLocalName());
        ml.artifactName = artifactElement.getAttributeValue(ATT_NAME);
        ml.lineNumber = mediatorElement.getLineNumber();
        return ml;
    }

    private static Set<QName> getAncestorQNameSet() {
        Set<QName> set = new HashSet<QName>();
        set.add(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "api"));
        set.add(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "proxy"));
        set.add(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sequence"));
        return set;
    }

    private static OMElement getFirstAncestorByName(OMElement element, Set<QName> qNames) {
        OMElement current = element;
        do {
            current = (OMElement) current.getParent();
        } while (!qNames.contains(current.getQName()) || current.getAttributeValue(ATT_NAME) == null);
        return current;
    }

    /**
     * Returns the artifact type where the mediator located.
     *
     * @return artifact type name
     */
    public String getArtifactType() {
        return artifactType.typeName;
    }

    /**
     * Gets the artifact name where the mediator is located.
     * @return
     */
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * Gets line number in the artifact configration file for mediator.
     *
     * @return line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}: {1}:{2}", artifactType, artifactName, lineNumber);
    }


    private enum ArtifactType {
        API("api"), PROXY_SERVICE("proxy", "proxyService"), SEQUENCE("sequence");

        private static final Map<String, ArtifactType> artifactTypes;

        static {
            artifactTypes = new HashMap<String, ArtifactType>();

            for (ArtifactType at : ArtifactType.values()) {
                artifactTypes.put(at.tagName, at);
            }
        }

        public static ArtifactType getArtifactType(String tagName) {
            return artifactTypes.get(tagName);
        }

        String tagName;
        String typeName;

        ArtifactType(String tagName) {
            this(tagName, tagName);
        }

        ArtifactType(String tagName, String typeName) {
            this.tagName = tagName;
            this.typeName = typeName;
        }

        public String getTagName() {
            return tagName;
        }

        public String getTypeName() {
            return typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }
}
