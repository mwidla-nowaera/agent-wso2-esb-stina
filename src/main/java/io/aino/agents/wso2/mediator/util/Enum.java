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

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerations used in {@link io.aino.agents.wso2.mediator.AinoMediator} logic.
 */
public class Enum {

    /**
     * Possible status values of transactions.
     *
     */
    public enum Status {
        SUCCESS("success"), FAILURE("failure"), UNKNOWN("unknown");

        private static final Map<String, Status> statuses;

        static {
            statuses = new HashMap<String, Status>();

            for (Status lg : Status.values()) {
                statuses.put(lg.statusString, lg);
            }
        }

        /**
         * Gets Enum member based on string representation.
         *
         * @param statusString status string
         * @return Enum member corresponding to statusString
         */
        public static Status getStatus(String statusString) {
            return statuses.get(statusString);
        }

        private final String statusString;

        Status(String statusString) {
            this.statusString = statusString;
        }

        @Override
        public String toString() {
            return statusString;
        }
    }


    /**
     * Possible directions on logging in mediator.
     * FROM direction means 'to' is marked as esb and 'from' is taken from configuration.
     * TO direction means that 'from' is marked as esb and 'to' is taken from configuration.
     */
    public enum ApplicationDirection {
        TO,
        FROM;

        public ApplicationDirection oppositeDirection() {
            if(TO == this) {
                return FROM;
            }
            return TO;
        }
    }
}
