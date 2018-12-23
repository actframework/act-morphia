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
import act.db.DbService;
import act.db.di.DaoInjectionListenerBase;
import org.mongodb.morphia.Datastore;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.List;

@SuppressWarnings("unused")
public class MorphiaDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class[] listenTo() {
        return new Class[] {MorphiaDaoBase.class};
    }

    @Override
    public void onInjection(Object bean, BeanSpec spec) {
        List<Type> typeParams = spec.typeParams();
        if (typeParams.isEmpty()) {
            typeParams = Generics.typeParamImplementations(spec.rawType(), MorphiaDaoBase.class);
        }
        if (typeParams.isEmpty()) {
            logger.warn("No type parameter information provided");
            return;
        }
        $.T2<Class, String> resolved = resolve(typeParams);
        DbService dbService = App.instance().dbServiceManager().dbService(resolved._2);
        if (dbService instanceof MorphiaService) {
            MorphiaService morphiaService = $.cast(dbService);
            MorphiaDaoBase dao = $.cast(bean);
            Datastore ds = morphiaService.ds();
            dao.ds(morphiaService.ds());
            dao.modelType(resolved._1);
        }
    }

}
