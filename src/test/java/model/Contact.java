package model;

import act.db.morphia.MorphiaDao;
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

    public static class Dao extends MorphiaDao<Contact> {
        public Dao() {
            super(Contact.class);
        }
    }

}
