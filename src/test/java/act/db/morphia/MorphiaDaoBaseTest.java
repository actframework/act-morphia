package act.db.morphia;

import act.db.Dao;
import act.test.util.Fixture;
import model.Contact;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.$;
import org.osgl.util.C;

import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MorphiaDaoBaseTest extends MongoTestBase {

    Contact.Dao dao;
    Fixture fixture;
    Contact tom;
    Contact peter;

    @Before
    public void prepareDao() {
        dao = new Contact.Dao();
        dao.setDatastore(ds());
        when(dbServiceManager.dao(any(Class.class))).thenAnswer(new Answer<Dao>() {
            @Override
            public Dao answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                Class<?> modelType = $.cast(args[0]);
                if (Contact.class.isAssignableFrom(modelType)) {
                    return dao;
                }
                return null;
            }
        });
        fixture = new Fixture(app);
        Map<String, Object> data = fixture.loadYamlFile("/test-data.yaml");
        tom = (Contact) data.get("tom");
        peter = (Contact) data.get("peter");
    }

    @Test
    public void testFindBy() {
        Iterable<Contact> contacts = dao.findBy("firstName, lastName", "Tom", "White");
        yes(contacts.iterator().hasNext());
        Contact c = contacts.iterator().next();
        eq("Tom", c.getFirstName());
        eq("White", c.getLastName());
    }

    @Test
    public void savedObjectShallBeFoundById() {
        ObjectId id = tom.getId();
        Contact contact1 = dao.findById(id);
        eq(contact1, tom);
    }

    @Test
    public void deletedObjectShallNotBeFoundById() {
        ObjectId id = tom.getId();
        dao.delete(tom);
        assertNull(dao.findById(id));
    }

    @Test
    public void itShallAllowDeleteAnEntityById() {
        ObjectId id = tom.getId();
        dao.deleteById(id);
        assertNull(dao.findById(id));
    }

    @Test
    public void testCount() {
        eq(2L, dao.count());
    }

    @Test
    public void testCountBy() {
        eq(1L, dao.countBy("firstName", "Tom"));
    }

    @Test
    public void reloadShallUpdateChanges() {
        Contact c = dao.findById(tom.getId());
        eq(c.getLastName(), tom.getLastName());
        tom.setLastName("Brown");
        dao.save(tom);
        ne(c.getLastName(), tom.getLastName());
        c = dao.reload(c);
        eq(c.getLastName(), tom.getLastName());
    }

    @Test
    public void testGetId() {
        eq(dao.getId(tom), tom.getId());
    }

    @Test
    public void testDeleteByQuery() {
        MorphiaQuery q = dao.q().filter("firstName", "Tom");
        dao.delete(q);
        assertNull(dao.findById(tom.getId()));
    }

    @Test
    public void testDeleteBy() {
        dao.deleteBy("firstName, lastName", "Tom", "Black");
        assertNotNull(dao.findById(tom.getId()));
        dao.deleteBy("firstName, lastName", "Tom", "White");
        assertNull(dao.findById(tom.getId()));
    }

    @Test
    public void testDrop() {
        dao.drop();
        assertEquals(0L, dao.count());
    }

    @Test
    public void testFindByIdList() {
        List<ObjectId> idList = C.list(tom.getId());
        List<Contact> contacts = C.list(dao.findByIdList(idList));
        eq(1, contacts.size());
        eq(tom, contacts.get(0));
    }
}
