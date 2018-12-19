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
import act.app.event.SysEventId;
import act.db.DbPlugin;
import act.db.DbService;
import act.db.morphia.annotation.PersistAsList;
import act.db.morphia.annotation.PersistAsMap;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.osgl.$;
import org.osgl.OsglConfig;
import org.osgl.util.C;
import org.osgl.util.converter.TypeConverterRegistry;
import osgl.version.Version;
import osgl.version.Versioned;

import java.lang.annotation.Annotation;
import java.util.*;

@Versioned
public class MorphiaPlugin extends DbPlugin {

    public static final Version VERSION = Version.of(MorphiaPlugin.class);

    /**
     * The key to fetch the {@link act.db.morphia.util.SequenceNumberGenerator}
     * db service ID configuration
     */
    public static final String CONF_KEY_SEQ_SVC_ID = "act_morphia_seqgen_svc_id";

    @Override
    protected void applyTo(final App app) {
        super.applyTo(app);
        app.jobManager().on(SysEventId.PRE_START, new Runnable() {
            @Override
            public void run() {
                MorphiaService.MorphiaServiceProvider provider = app.getInstance(MorphiaService.MorphiaServiceProvider.class);
                app.injector().registerProvider(MorphiaService.class, provider);
                app.injector().registerNamedProvider(MorphiaService.class, provider);
            }
        });
    }

    @Override
    public Set<Class<? extends Annotation>> entityAnnotations() {
        Set<Class<? extends Annotation>> annoTypes = new HashSet<>();
        annoTypes.add(Entity.class);
        return annoTypes;
    }

    public MorphiaPlugin() {
        OsglConfig.registerImmutableClassNames(C.list(ObjectId.class.getName()));
        MorphiaLoggerFactory.reset();
        MorphiaLoggerFactory.registerLogger(ActMorphiaLogger.Factory.class);
        MappedField.addInterestingAnnotation(PersistAsList.class);
        MappedField.addInterestingAnnotation(PersistAsMap.class);
        TypeConverterRegistry.INSTANCE.register(new $.TypeConverter<ObjectId, String>() {
            @Override
            public String convert(ObjectId o) {
                return o.toHexString();
            }
        }).register(new $.TypeConverter<String, ObjectId>() {
            @Override
            public ObjectId convert(String s) {
                return new ObjectId(s);
            }
        })
        ;
    }

    public DbService initDbService(String id, App app, Map<String, String> conf) {
        return new MorphiaService(id, app, conf);
    }



}
