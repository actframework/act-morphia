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
