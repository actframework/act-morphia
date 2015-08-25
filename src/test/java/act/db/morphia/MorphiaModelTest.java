package act.db.morphia;

import model.Contact;
import org.junit.Before;
import org.junit.Test;

public class MorphiaModelTest extends MongoTestBase {

    Contact.Dao dao;

    @Before
    public void prepareDao() {
        dao = new Contact.Dao();
        dao.setApp(app);
        dao.setDatastore(ds());
    }

    @Test
    public void modelIsNotNewAfterSaved() {
        Contact contact = new Contact("Tom", "White", "1234");
        yes(contact._isNew());
        dao.save(contact);
        no(contact._isNew());
    }
}
