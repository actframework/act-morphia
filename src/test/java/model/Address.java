package model;

import org.mongodb.morphia.annotations.Embedded;

@Embedded
public class Address {
    private String unit;
    private String street;
    private String suburb;
    private String state;

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (unit != null ? !unit.equals(address.unit) : address.unit != null) return false;
        if (street != null ? !street.equals(address.street) : address.street != null) return false;
        if (suburb != null ? !suburb.equals(address.suburb) : address.suburb != null) return false;
        return !(state != null ? !state.equals(address.state) : address.state != null);

    }

    @Override
    public int hashCode() {
        int result = unit != null ? unit.hashCode() : 0;
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (suburb != null ? suburb.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }
}
