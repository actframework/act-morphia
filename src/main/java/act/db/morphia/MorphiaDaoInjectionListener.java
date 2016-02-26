package act.db.morphia;

import act.app.App;
import act.db.Dao;
import act.db.DbService;
import act.di.guice.DaoInjectionListenerBase;
import org.osgl.$;

public class MorphiaDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class<? extends Dao> targetDaoType() {
        return MorphiaDaoBase.class;
    }

    @Override
    public void afterInjection(Dao dao) {
        DbService dbService = App.instance().dbServiceManager().dbService(svcId());
        if (dbService instanceof MorphiaService) {
            MorphiaDao morphiaDao = $.cast(dao);
            MorphiaService ebeanService = $.cast(dbService);
            morphiaDao.ds(ebeanService.ds());
            morphiaDao.modelType(modelType());
        }
    }
}
