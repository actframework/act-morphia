package act.db.morphia;

import model.Contact;
import org.junit.Before;
import org.junit.Test;

public class ObjectIdMorphiaModelBaseTest extends MorphiaDaoTestBase<Contact> {

    @Override
    protected Class<Contact> entityClass() {
        return Contact.class;
    }

    @Test
    public void modelIsNotNewAfterSaved() {
        Contact contact = new Contact("Tom", "White", "1234");
        yes(contact._isNew());
        dao.save(contact);
        no(contact._isNew());
    }
}
