package io.clone.dereva.models;

/**
 * Created by walter on 11/9/17.
 */

public class User {
    private String names, email, password, phone;

    public User() {
    }

    public User(String names, String email, String password, String phone) {
        this.names = names;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
