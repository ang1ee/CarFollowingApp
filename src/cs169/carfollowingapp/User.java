package cs169.carfollowingapp;

/**
 * Created by Steven on 3/8/14.
 */
public class User {
    private String username;
    private String password;

    public void setUsername(String name) {
        username = name;
    }

    public void setPassword(String pw) {
        password = pw;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
