package org.wstorm.rcache.serializer;

import org.wstorm.rcache.RObject;

public class SObject implements RObject<String> {


    String id;

    int sum;

    public SObject() { //kryo need it

    }

    public SObject(String id, int sum) {
        this.id = id;
        this.sum = sum;
    }

    public String getId() {
        return id();
    }

    public int getSum() {
        return sum;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isBlank() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SObject)) return false;

        SObject sObject = (SObject) o;

        return sum == sObject.getSum() && (getId() != null ? id.equals(sObject.id) : sObject.id == null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + sum;
        return result;
    }
}