package act.db.morphia.event;

import act.event.ActEvent;
import org.mongodb.morphia.mapping.Mapper;

/**
 * The event triggered after entity classes has been mapped
 */
public class EntityMapped extends ActEvent<Mapper> {

    public EntityMapped(Mapper source) {
        super(source);
    }
}
