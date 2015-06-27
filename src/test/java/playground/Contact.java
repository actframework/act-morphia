package playground;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("ctct")
public class Contact {
    @Id
    private String id;

    public String id() {
        return id;
    }
}
