package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.LoggerFactory;
import org.osgl.logging.L;

class ActMorphiaLogger implements Logger {

    private org.osgl.logging.Logger logger;

    private ActMorphiaLogger(Class<?> targetClass) {
        logger = L.get(targetClass);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object... arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(t, msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object... arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(t, msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object... arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(t, msg);
    }

    @Override
    public boolean isWarningEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warning(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warning(String format, Object... arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warning(String msg, Throwable t) {
        logger.warn(t, msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format, Object... arg) {
        logger.error(format, arg);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(t, msg);
    }

    static class Factory implements LoggerFactory {
        @Override
        public Logger get(Class<?> c) {
            return new ActMorphiaLogger(c);
        }
    }
}
