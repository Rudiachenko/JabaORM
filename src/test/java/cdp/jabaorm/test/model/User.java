package cdp.jabaorm.test.model;

import cdp.jabaorm.annotations.JabaColumn;
import cdp.jabaorm.annotations.JabaEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@JabaEntity(tableName = "user")
public class User {

    @JabaColumn(columnName = "user_id")
    private Integer userId;

    @JabaColumn
    private String name;

    @JabaColumn(columnName = "birthdate")
    private Date birthDate;

    @JabaColumn(columnName = "is_man")
    private Boolean isMan;

    // Just ignored field
    private String address;

    public User() {
    }
}
