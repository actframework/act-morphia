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

import act.Act;
import act.ActComponent;
import act.app.App;
import act.app.event.AppEventId;
import act.util.SubTypeFinder;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.converters.TypeConverter;

public class TypeConverterFinder extends SubTypeFinder<TypeConverter> {

    private static final String SYS_CONVERTER_PKG = Morphia.class.getPackage().getName();

    public TypeConverterFinder() {
        super(TypeConverter.class);
    }

    @Override
    protected void found(final Class<? extends TypeConverter> target, final App app) {
        if (target.getName().startsWith(SYS_CONVERTER_PKG)) {
            return;
        }
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                MorphiaService.mapper().getConverters().addConverter(app.getInstance(target));
            }
        });
    }

}
