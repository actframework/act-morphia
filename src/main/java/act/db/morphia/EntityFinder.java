package act.db.morphia;

import act.app.App;
import act.app.AppByteCodeScanner;
import act.util.AnnotatedTypeFinder;
import org.mongodb.morphia.annotations.Entity;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

import java.util.Map;
import java.util.Set;

public class EntityFinder extends AnnotatedTypeFinder {
    public EntityFinder() {
        super(true, false, Entity.class, new _.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(App app, String className) throws NotAppliedException, _.Break {
                MorphiaService.morphia().map(_.classForName(className, app.classLoader()));
                return null;
            }
        });
    }
}
