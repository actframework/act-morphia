package act.db.morphia.util;

import act.app.App;
import act.app.DbServiceManager;
import act.db.morphia.MorphiaDao;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.GenericTypedBeanLoader;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Type;
import java.util.List;

@ApplicationScoped
public class MorphiaDaoLoader implements GenericTypedBeanLoader<MorphiaDao> {

    private DbServiceManager dbServiceManager;
    public MorphiaDaoLoader() {
        dbServiceManager = App.instance().dbServiceManager();
    }

    @Override
    public MorphiaDao load(BeanSpec beanSpec) {
        List<Type> typeList = beanSpec.typeParams();
        int sz = typeList.size();
        if (sz > 0) {
            Class<?> modelType = BeanSpec.rawTypeOf(typeList.get(0));
            return (MorphiaDao) dbServiceManager.dao(modelType);
        }
        return null;
    }
}
