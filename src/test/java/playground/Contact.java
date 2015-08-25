package playground;

import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.osgl._;

@Entity("ctct")
public class Contact extends MorphiaModel {

    private String firstName;
    private String lastName;
    private String mobile;

    private Contact() {

    }

    public Contact(String firstName, String lastName, String mobile) {
        this.firstName = _.NPE(firstName);
        this.lastName = _.NPE(lastName);
        this.mobile = _.NPE(mobile);
    }

    private Address address;

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

    public static class Dao extends MorphiaDao<ObjectId, Contact, Dao> {
        public Dao() {
            super(Contact.class);
        }
    }

}
