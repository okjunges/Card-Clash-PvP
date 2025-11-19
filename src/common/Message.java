package common;

import javax.swing.*;
import java.io.Serializable;

public class Message implements Serializable {
    public final static int MODE_LOGIN = 0x1;
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_FILE = 0x20;
    public final static int MODE_TX_IMAGE = 0x40;

    private String userID;
    private int mode;
    private String message;
    private ImageIcon image;
    private long size;



    public Message(String userID, int code, String message, ImageIcon image, long size) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public Message(String userID, int code, String message, ImageIcon image) {
        this(userID, code, message, image, 0);
    }

    public Message(String userID, int code) {
        this(userID, code, null, null);
    }

    public Message(String userID, int code, String message) {
        this(userID, code, message, null);
    }

    public Message(String userID, int code, ImageIcon image) {
        this(userID, code, null, image);
    }

    public Message(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size);
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}