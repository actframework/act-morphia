package act.db.morphia.util;

import act.app.App;
import act.app.DbServiceManager;
import act.db.morphia.MorphiaDaoBase;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.GenericTypedBeanLoader;

import javax.enterprise.context.ApplicationScoped;
import java.lang.reflect.Type;
import java.util.List;

@ApplicationScoped
public class MorphiaDaoBaseLoader implements GenericTypedBeanLoader<MorphiaDaoBase> {

    private DbServiceManager dbServiceManager;
    public MorphiaDaoBaseLoader() {
        dbServiceManager = App.instance().dbServiceManager();
    }

    @Override
    public MorphiaDaoBase load(BeanSpec beanSpec) {
        List<Type> typeList = beanSpec.typeParams();
        int sz = typeList.size();
        if (sz > 1) {
            Class<?> modelType = BeanSpec.rawTypeOf(typeList.get(1));
            return (MorphiaDaoBase) dbServiceManager.dao(modelType);
        }
        return null;
    }
}
