package model;

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

import act.db.morphia.MorphiaModel;
import org.mongodb.morphia.annotations.Entity;
import org.osgl.$;

@Entity("ctct")
public class Contact extends MorphiaModel<Contact> {

    private String firstName;
    private String lastName;
    private String mobile;
    private Address address;

    public Contact() {

    }

    public Contact(String firstName, String lastName, String mobile) {
        this.firstName = $.NPE(firstName);
        this.lastName = $.NPE(lastName);
        this.mobile = $.NPE(mobile);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Class<Contact> modelType() {
        return super.modelType();
    }

}
