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

import org.apache.synapse.mediators.builtin.LogMediator;

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
     * Logging categories.
     * FATAL, ERROR, WARN, INFO, DEBUG, TRACE.
     */
    public enum LogCategory {
        FATAL(LogMediator.CATEGORY_FATAL, "FATAL"), ERROR(LogMediator.CATEGORY_ERROR, "ERROR"), WARN(
                LogMediator.CATEGORY_WARN, "WARN"), INFO(LogMediator.CATEGORY_INFO, "INFO"), DEBUG(
                LogMediator.CATEGORY_DEBUG, "DEBUG"), TRACE(LogMediator.CATEGORY_TRACE, "TRACE");

        private static final Map<String, LogCategory> logCategories;

        static {
            logCategories = new HashMap<String, LogCategory>();

            for (LogCategory lg : LogCategory.values()) {
                logCategories.put(lg.categoryString, lg);
            }
        }
        /**
         * Gets Enum member based on string representation.
         *
         * @param categoryString category string
         * @return Enum member corresponding to categoryString
         */

        public static LogCategory getLogCategory(String categoryString) {
            return logCategories.get(categoryString);
        }

        private final int categoryInt;
        private final String categoryString;

        LogCategory(int categoryInt, String categoryString) {
            this.categoryInt = categoryInt;
            this.categoryString = categoryString;
        }

        public int getCategoryInt() {
            return categoryInt;
        }

        @Override
        public String toString() {
            return categoryString;
        }
    }

    /**
     * Enum for log level.
     * SIMPLE, HEADERS, FULL, CUSTOM.
     */
    public enum LogLevel {
        SIMPLE(LogMediator.SIMPLE, "simple"), HEADERS(LogMediator.HEADERS, "headers"), FULL(LogMediator.FULL, "full"), CUSTOM(
                LogMediator.CUSTOM, "custom");

        private static final Map<String, LogLevel> logLevels;

        static {
            logLevels = new HashMap<String, LogLevel>();

            for (LogLevel lv : LogLevel.values()) {
                logLevels.put(lv.levelString, lv);
            }
        }

        public static LogLevel getLogLevel(String levelString) {
            return logLevels.get(levelString);
        }

        private final int levelInt;
        private final String levelString;

        LogLevel(int levelInt, String levelString) {
            this.levelInt = levelInt;
            this.levelString = levelString;
        }

        public int getLevelInt() {
            return levelInt;
        }

        @Override
        public String toString() {
            return levelString;
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
