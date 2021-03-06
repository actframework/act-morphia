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
import act.aaa.AAAService;
import act.aaa.PasswordVerifier;
import act.aaa.util.AAALookup;
import act.aaa.util.PrivilegeCache;
import act.apidoc.SampleData;
import act.apidoc.SampleDataCategory;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.validation.Password;
import org.osgl.$;
import org.osgl.aaa.*;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.StringTokenSet;

import java.util.*;
import javax.persistence.Column;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

public abstract class MorphiaUserBase<T extends MorphiaUserBase> extends MorphiaAdaptiveRecord<T> implements Principal, UserLinked {

    @Transient
    private transient Map<String, String> _properties = new HashMap<>();

    @NotNull
    @Column(unique = true, nullable = false, updatable = false)
    public String email;

    @SampleData.Category(SampleDataCategory.PRIVILEGE)
    private int privilege;

    @SampleData.Category(SampleDataCategory.PERMISSIONS)
    private String permissions;

    @SampleData.Category(SampleDataCategory.ROLES)
    private String roles;

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

    /**
     * This method is deprecated. Please use {@link #grantPrivilege(int)} instead
     */
    @Deprecated
    public T setPrivilege(int privilege) {
        this.privilege = privilege;
        return me();
    }

    public T grantPrivilege(int privilege) {
        this.privilege = privilege;
        return me();
    }

    public T grantPrivilege(Privilege privilege) {
        this.privilege = privilege.getLevel();
        return me();
    }

    public T grantPermissions(Permission... permissions) {
        if (permissions.length > 0) {
            return grantPermissionByNames(stringOf(C.Array.of(permissions)));
        }
        return me();
    }

    public T grantPermissionByNames(String... permissions) {
        AAAService aaaService = aaaService();
        for (String perm : permissions) {
            if (!aaaService.isValidPermission(perm)) {
                throw new IllegalArgumentException("Permission not recognized: " + perm);
            }
        }
        this.permissions = StringTokenSet.merge(this.permissions, permissions);
        return me();
    }

    public T grantPermissions(Collection<Permission> permissions) {
        if (permissions.isEmpty()) {
            return me();
        }
        return grantPermissionByNames(stringOf(permissions));
    }

    public T grantPermissionByNames(Collection<String> permissions) {
        if (permissions.isEmpty()) {
            return me();
        }
        return this.grantPermissionByNames(permissions.toArray(new String[]{}));
    }

    public T grantRoles(Role... roles) {
        if (roles.length > 0) {
            return grantRoleByNames(stringOf(C.Array.of(roles)));
        }
        return me();
    }

    public T grantRoleByNames(String... roles) {
        AAAService aaaService = aaaService();
        for (String role : roles) {
            if (!aaaService.isValidRole(role)) {
                throw new IllegalArgumentException("Role not recognized: " + role);
            }
        }
        this.roles = StringTokenSet.merge(this.roles, roles);
        return me();
    }

    public T grantRoles(Collection<Role> roles) {
        if (roles.isEmpty()) {
            return me();
        }
        return grantRoleByNames(stringOf(roles));
    }

    public T grantRoleByNames(Collection<String> roles) {
        if (roles.isEmpty()) {
            return me();
        }
        return this.grantRoleByNames(roles.toArray(new String[]{}));
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public void setProperty(String key, String value) {
        props().put(key, value);
    }

    @Override
    public void unsetProperty(String key) {
        props().remove(key);
    }

    @Override
    public String getProperty(String key) {
        if ("id".equals(key)) {
            Object o = $.getProperty(this, "id");
            return S.string(o);
        }
        return props().get(key);
    }

    @Override
    public Set<String> propertyKeys() {
        return _properties.keySet();
    }

    @Override
    public String toString() {
        return S.blank(email) ? "unknown-user" : email;
    }

    private Map<String, String> props() {
        if (null == _properties) {
            _properties = new HashMap<>();
        }
        return _properties;
    }

    protected final T me() {
        return (T) this;
    }

    private static String stringOf(Iterable<? extends AAAObject> aaaObjects) {
        S.Buffer buf = S.buffer();
        boolean first = true;
        for (AAAObject obj : aaaObjects) {
            if (first) {
                buf.append(StringTokenSet.SEPARATOR);
                first = false;
            }
            buf.append(obj.getName());
        }
        return buf.toString();
    }

    private AAAService aaaService() {
        return Act.getInstance(AAAService.class);
    }

}
