package io.clone.dereva.models;

/**
 * Created by walter on 11/22/17.
 */

public class Sender {
    public Notification mNotification;
    public String to;

    public Sender() {
    }

    public Sender(String to,Notification notification) {
        this.mNotification = notification;
        this.to = to;
    }

    public Notification getNotification() {
        return mNotification;
    }

    public void setNotification(Notification notification) {
        this.mNotification = notification;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
