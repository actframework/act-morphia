package act.db.morphia;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.event.AppEventId;
import act.util.AnnotatedTypeFinder;
import act.util.SubTypeFinder2;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import java.util.Map;
import java.util.Set;

@ActComponent
public class TypeConverterFinder extends SubTypeFinder2<TypeConverter> {

    private static final String SYS_CONVERTER_PKG = Morphia.class.getPackage().getName();

    public TypeConverterFinder() {
        super(TypeConverter.class);
    }

    @Override
    protected void found(final Class<TypeConverter> target, final App app) {
        if (target.getName().startsWith(SYS_CONVERTER_PKG)) {
            return;
        }
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_LOADED, new Runnable() {
            @Override
            public void run() {
                MorphiaService.mapper().getConverters().addConverter(app.newInstance(target));
            }
        });
    }

}
