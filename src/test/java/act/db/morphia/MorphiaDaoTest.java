package act.db.morphia;

/*-
 * #%L
 * ACT Morphia
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import model.Contact;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.osgl.util.C;

import java.util.List;

/**
 * Test CRUD and search features
 */
public class MorphiaDaoTest extends MorphiaDaoTestBase<Contact> {

    Contact tom;
    Contact peter;

    @Override
    protected void postPrepareData() {
        tom = (Contact) initData.get("tom");
        peter = (Contact) initData.get("peter");
    }

    @Override
    protected Class entityClass() {
        return Contact.class;
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
    public void fetchedObjectShallReturnCorrectModelType() {
        ObjectId id = tom.getId();
        Contact contact1 = dao.findById(id);
        eq(Contact.class, contact1.modelType());
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
