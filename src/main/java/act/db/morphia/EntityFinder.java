package act.db.morphia;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.util.AnnotatedTypeFinder;
import org.mongodb.morphia.annotations.Entity;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

@ActComponent
public class EntityFinder extends AnnotatedTypeFinder {
    public EntityFinder() {
        super(true, true, Entity.class, new _.F2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>>() {
            @Override
            public Map<Class<? extends AppByteCodeScanner>, Set<String>> apply(App app, String className) throws NotAppliedException, _.Break {
                Class<?> c = _.classForName(className, app.classLoader());
                MorphiaService.morphia().map(c);
                return null;
            }
        });
    }
}
