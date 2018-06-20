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

import act.Act;
import act.aaa.PasswordVerifier;
import act.aaa.util.AAALookup;
import act.aaa.util.PrivilegeCache;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.validation.Password;
import org.osgl.$;
import org.osgl.aaa.Permission;
import org.osgl.aaa.Principal;
import org.osgl.aaa.Privilege;
import org.osgl.aaa.Role;
import org.osgl.util.S;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class MorphiaUserBase<T extends MorphiaUserBase> extends MorphiaAdaptiveRecord<T> implements Principal, UserLinked {

    @NotNull
    public String email;

    public int privilege;

    public String permissions;

    public String roles;

    @Password
    private char[] password;

    public void setPassword(char[] password) {
        this.password = Act.crypto().passwordHash(password);
        $.resetArray(password);
    }

    @PasswordVerifier
    public boolean verifyPassword(char[] password) {
        return Act.crypto().verifyPassword(password, this.password);
    }

    @Override
    public boolean isLinkedTo(Principal user) {
        return S.eq(email, user.getName());
    }

    @Override
    public Privilege getPrivilege() {
        return PrivilegeCache.get(privilege);
    }

    @Override
    public List<Role> getRoles() {
        return AAALookup.roles(roles);
    }

    @Override
    public List<Permission> getPermissions() {
        return AAALookup.permissions(permissions);
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public void setProperty(String key, String value) {
        putValue(key, value);
    }

    @Override
    public void unsetProperty(String key) {
        putValue(key, null);
    }

    @Override
    public String getProperty(String key) {
        return getValue(key);
    }

    @Override
    public Set<String> propertyKeys() {
        return keySet();
    }

    @Override
    public String toString() {
        return S.blank(email) ? "unknown-user" : email;
    }

}
