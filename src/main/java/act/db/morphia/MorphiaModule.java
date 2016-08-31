package act.db.morphia;

import act.db.morphia.util.MorphiaDaoBaseLoader;
import act.db.morphia.util.MorphiaDaoLoader;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.inject.Module;

@SuppressWarnings("unused")
public class MorphiaModule extends Module {

    @Override
    protected void configure() {
        bind(Mapper.class).to(MorphiaService.mapper());
        bind(Morphia.class).to(MorphiaService.morphia());

        registerGenericTypedBeanLoader(MorphiaDaoBase.class, new MorphiaDaoBaseLoader());
        registerGenericTypedBeanLoader(MorphiaDao.class, new MorphiaDaoLoader());
    }

}
