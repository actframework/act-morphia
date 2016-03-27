package act.db.morphia;

import act.app.App;
import act.db.DbService;
import act.db.di.DaoInjectionListenerBase;
import org.osgl.$;

import java.lang.reflect.Type;

@SuppressWarnings("unused")
public class MorphiaDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class[] listenTo() {
        return new Class[] {MorphiaDao.class, MorphiaDaoBase.class};
    }

    @Override
    public void onInjection(Object injectee, Type[] typeParameters) {
        if (null == typeParameters) {
            logger.warn("No type parameter information provided");
            return;
        }
        $.T2<Class, String> resolved = resolve(typeParameters);
        DbService dbService = App.instance().dbServiceManager().dbService(resolved._2);
        if (dbService instanceof MorphiaService) {
            MorphiaService morphiaService = $.cast(dbService);
            MorphiaDaoBase dao = $.cast(injectee);
            dao.ds(morphiaService.ds());
            dao.modelType(resolved._1);
        }
    }

}
