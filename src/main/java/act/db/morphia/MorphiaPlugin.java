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

import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;
import act.db.morphia.annotation.PersistAsList;
import act.db.morphia.annotation.PersistAsMap;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;
import osgl.version.Version;

import java.util.Map;

public class MorphiaPlugin extends DbPlugin {

    public static final Version VERSION = Version.of(MorphiaPlugin.class);

    /**
     * The key to fetch the {@link act.db.morphia.util.SequenceNumberGenerator}
     * db service ID configuration
     */
    public static final String CONF_KEY_SEQ_SVC_ID = "act_morphia_seqgen_svc_id";

    public MorphiaPlugin() {
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(ActMorphiaLogger.Factory.class);
        MappedField.addInterestingAnnotation(PersistAsList.class);
        MappedField.addInterestingAnnotation(PersistAsMap.class);
    }

    public DbService initDbService(String id, App app, Map<String, String> conf) {
        return new MorphiaService(id, app, conf);
    }



}
