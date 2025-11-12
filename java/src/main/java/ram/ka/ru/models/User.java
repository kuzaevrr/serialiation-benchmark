// User.java
package ram.ka.ru.models;

import java.util.List;

public class User {
    private String id;
    private String name;
    private String email;
    private int age;
    private boolean active;
    private List<String> roles;
    private double balance;
    
    public User() {}
    
    public User(String id, String name, String email, int age, 
                boolean active, List<String> roles, double balance) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.active = active;
        this.roles = roles;
        this.balance = balance;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}