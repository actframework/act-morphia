package act.db.morphia;

import act.app.App;
import act.db.DbService;
import act.db.di.DaoInjectionListenerBase;
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
            dao.ds(morphiaService.ds());
            dao.modelType(resolved._1);
        }
    }

}
