package act.db.morphia.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a {@link org.osgl.util.KVStore} typed field indicate the
 * data should be persisted as a Map instead of (K,V) pair list
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface PersistAsMap {
}
