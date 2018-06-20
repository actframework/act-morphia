package act.aaa.model;

/*-
 * #%L
 * ACT AAA Plugin
 * %%
 * Copyright (C) 2015 - 2018 ActFramework
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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.osgl.aaa.Principal;

public abstract class MorphiaAuditBase extends AuditBase {

    @Id
    public ObjectId id;

    public MorphiaAuditBase() {}

    public MorphiaAuditBase(Object aTarget, Principal aPrincipal, String aPermission, String aPrivilege, boolean theSuccess, String aMessage) {
        super(aTarget, aPrincipal, aPermission, aPrivilege, theSuccess, aMessage);
    }

}
