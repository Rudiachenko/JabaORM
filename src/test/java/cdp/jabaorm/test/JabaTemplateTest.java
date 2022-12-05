package cdp.jabaorm.test;

import cdp.jabaorm.JabaTemplate;
import cdp.jabaorm.test.model.Car;
import cdp.jabaorm.test.model.Employee;
import cdp.jabaorm.test.model.User;
import cdp.jabaorm.util.ConnectionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class JabaTemplateTest {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    private static final Connection connection = ConnectionUtil.getConnection();

    static {
        if (connection == null) {
            throw new RuntimeException("Connection is not established");
        }

        try {
            createUserTable();

            createAutomobileTable();

            createEmployeeTable();
        } catch (Exception e) {
            System.out.println("Fail while creating tables");
        }
    }

    @Test
    public void userCrudTest() throws Exception {
        JabaTemplate<User> template = JabaTemplate.create(User.class, connection);

        // User to save
        User user = new User(1, "Bob", sdf.parse("31-07-1997"), true, null);

        template.save(user);

        PreparedStatement statement = connection.prepareStatement("select * from user");
        ResultSet resultSet = statement.executeQuery();

        // Check that user was saved
        while (resultSet.next()) {
            Assert.assertEquals(1, resultSet.getInt("user_id"));
            Assert.assertEquals("Bob", resultSet.getString("name"));
            Assert.assertEquals(Boolean.TRUE, resultSet.getBoolean("is_man"));
        }

        resultSet.close();

        // Search user by id
        User userToGet = new User(1, null, null, null, null);
        User userFromDb = template.get(userToGet);

        // Check that user is got
        Assert.assertEquals(user, userFromDb);

        // Update ID to the user
        User userWitchChangedId = new User(2, null, null, null, null);

        // Search user by id and update id
        template.update(userToGet, userWitchChangedId);

        // Check that user was updated
        userFromDb = template.get(userWitchChangedId);
        Assert.assertEquals(Integer.valueOf(2), userFromDb.getUserId());

        // Delete user with updated id
        template.delete(userFromDb);

        resultSet = statement.executeQuery();

        // Check that table is empty
        Assert.assertFalse(resultSet.next());
    }

    @Test
    public void carCrudTest() throws Exception {
        JabaTemplate<Car> template = JabaTemplate.create(Car.class, connection);

        Car car = new Car("BMW", sdf.parse("31-07-1997"), 249, "Sea blue");
        template.save(car);

        Car carFromDb = template.get(new Car("BMW", null, null, null));
        Assert.assertEquals(car, carFromDb);

        int updatedRows = template.update(carFromDb, new Car(null, null, 300, null));
        carFromDb = template.get(new Car("BMW", null, null, null));

        car.setPower(300);
        Assert.assertEquals(car, carFromDb);
        Assert.assertEquals(1, updatedRows);

        template.delete(car);
    }

    @Test
    public void employeeCrudTest() throws Exception {
        JabaTemplate<Employee> template = JabaTemplate.create(Employee.class, connection);

        Employee employee = new Employee("Bob", "Alison", "Software engineer", 3);
        template.save(employee);

        Employee employeeFromDb = template.get(new Employee("Bob", null, null, null));
        Assert.assertEquals(employee, employeeFromDb);

        int updatedRows = template.update(employeeFromDb, new Employee(null, null, null, 4));
        employeeFromDb = template.get(new Employee("Bob", null, null, null));

        employee.setExperienceInYears(4);
        Assert.assertEquals(employee, employeeFromDb);
        Assert.assertEquals(1, updatedRows);

        template.delete(employee);
    }

    private static void createUserTable() throws SQLException {
        String userTableQuery = "CREATE TABLE user (user_id INT, name VARCHAR(255), birthdate DATE, is_man BOOLEAN);";
        PreparedStatement statement = connection.prepareStatement(userTableQuery);
        statement.execute();
        statement.close();
    }

    private static void createAutomobileTable() throws SQLException {
        String carTableQuery = "CREATE TABLE automobile (brand VARCHAR(255), manufacture_name DATE , power INT, colour VARCHAR(255));";
        PreparedStatement statement = connection.prepareStatement(carTableQuery);
        statement.execute();
        statement.close();
    }
    private static void createEmployeeTable() throws SQLException {
        String employeeTableQuery = "CREATE TABLE employee (name VARCHAR(255), surname VARCHAR(255), position VARCHAR(255), experience INT);";
        PreparedStatement statement = connection.prepareStatement(employeeTableQuery);
        statement.execute();
        statement.close();
    }
}
