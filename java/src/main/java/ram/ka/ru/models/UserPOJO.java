package ram.ka.ru.models;

// POJO класс
public class UserPOJO {
    public String id, name, email;
    public int age;
    public boolean active;
    public String[] roles;
    public double balance;
    // Конструкторы и геттеры/сеттеры опущены для краткости
    public UserPOJO(String id, String name, String email, int age, boolean active, String[] roles, double balance) {
        this.id = id; this.name = name; this.email = email; this.age = age;
        this.active = active; this.roles = roles; this.balance = balance;
    }
}