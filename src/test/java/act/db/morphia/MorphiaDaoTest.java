package act.db.morphia;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import playground.Contact;

public class MorphiaDaoTest extends MongoTestBase {

    Contact.Dao dao;

    @Before
    public void prepareDao() {
        dao = new Contact.Dao();
        dao.setApp(app);
        dao.setDatastore(ds());
    }

    @Test
    public void savedObjectShallBeFoundById() {
        Contact contact = new Contact("Tom", "White", "1342");
        dao.save(contact);
        ObjectId id = contact.getId();
        Contact contact1 = dao.findById(id);
        eq(contact1, contact);
    }

    @Test
    public void deletedObjectShallNotBeFoundById() {
        Contact contact = new Contact("Tom", "White", "1342");
        dao.save(contact);
        ObjectId id = contact.getId();
        dao.delete(contact);
        assertNull(dao.findById(id));
    }

    @Test
    public void itShallAllowDeleteAnEntityById() {

    }


}
