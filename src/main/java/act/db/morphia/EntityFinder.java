package act.db.morphia;

import act.ActComponent;
import act.app.event.AppEventId;
import act.db.morphia.event.EntityMapped;
import act.util.AnnotatedTypeFinder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;

@ActComponent
@SuppressWarnings("unused")
public class EntityFinder extends AnnotatedTypeFinder {

    public EntityFinder() {
        super(Entity.class);
        app().jobManager().on(AppEventId.PRE_START, new Runnable() {
            @Override
            public void run() {
                doMap();
            }
        });
    }

    private void doMap() {
        Mapper mapper = MorphiaService.mapper();
        for (String className : foundClasses) {
            Class<?> c = $.classForName(className, app().classLoader());
            if (!mapper.isMapped(c)) {
                mapper.addMappedClass(c);
            }
        }
        app().eventBus().emit(new EntityMapped(mapper));
    }
}
