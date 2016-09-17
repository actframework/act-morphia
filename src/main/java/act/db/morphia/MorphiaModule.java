package act.db.morphia;

import act.app.event.AppEventId;
import act.db.morphia.event.EntityMapped;
import act.job.OnAppEvent;
import act.util.AnnotatedClassFinder;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;
import org.osgl.inject.Module;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static act.Act.app;
import static act.db.morphia.MorphiaService.getService;
import static act.db.morphia.MorphiaService.mapper;

@SuppressWarnings("unused")
public class MorphiaModule extends Module {

    @Override
    protected void configure() {
        bind(Mapper.class).to(mapper());
        bind(Morphia.class).to(MorphiaService.morphia());
    }

    @AnnotatedClassFinder(Entity.class)
    @SuppressWarnings("unused")
    public void autoMapEntity(Class<?> clz) {
        Mapper mapper = mapper();
        mapper.addMappedClass(clz);

        registerFieldNameMapping(clz);
    }

    @OnAppEvent(AppEventId.PRE_START)
    @SuppressWarnings("unused")
    public void raiseMappedEvent() {
        app().eventBus().emit(new EntityMapped(mapper()));
    }

    private void registerFieldNameMapping(Class<?> clz) {
        Map<String, String> mapping = findFieldNameMapping(clz);
        if (!mapping.isEmpty()) {
            MorphiaService dbService = getService(clz);
            dbService.registerFieldNameMapping(clz, mapping);
        }
    }

    private Map<String, String> findFieldNameMapping(Class<?> clz) {
        Map<String, String> mapping = new HashMap<String, String>();
        List<Field> fields = $.fieldsOf(clz, true);
        for (Field f : fields) {
            Property property = f.getAnnotation(Property.class);
            if (null != property) {
                mapping.put(f.getName(), property.value());
            }
        }
        return mapping;
    }

}
