package act.db.morphia.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When an model entity class is marked with `NoQueryValidation` annotation
 * the corresponding Dao will disable query validation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NoQueryValidation {
}
