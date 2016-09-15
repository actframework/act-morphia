package act.db.morphia;

import act.app.event.AppEventId;
import act.db.morphia.event.EntityMapped;
import act.db.morphia.util.MorphiaDaoBaseLoader;
import act.db.morphia.util.MorphiaDaoLoader;
import act.job.OnAppEvent;
import act.util.AnnotatedClassFinder;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.inject.Module;

import static act.Act.app;
import static act.db.morphia.MorphiaService.mapper;

@SuppressWarnings("unused")
public class MorphiaModule extends Module {

    @Override
    protected void configure() {
        bind(Mapper.class).to(mapper());
        bind(Morphia.class).to(MorphiaService.morphia());

        registerGenericTypedBeanLoader(MorphiaDaoBase.class, new MorphiaDaoBaseLoader());
        registerGenericTypedBeanLoader(MorphiaDao.class, new MorphiaDaoLoader());
    }

    @AnnotatedClassFinder(Entity.class)
    @SuppressWarnings("unused")
    public void autoMapEntity(Class<?> clz) {
        Mapper mapper = mapper();
        if (!mapper.isMapped(clz)) {
            mapper.addMappedClass(clz);
        }
    }

    @OnAppEvent(AppEventId.PRE_START)
    @SuppressWarnings("unused")
    public void raiseMappedEvent() {
        app().eventBus().emit(new EntityMapped(mapper()));
    }


}
