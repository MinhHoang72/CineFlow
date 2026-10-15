package com.mycompany.cinechain.model;

public class Branch {

    private final int id;
    private final String name, address, city;

    public Branch(int id, String name, String address, String city) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
}
