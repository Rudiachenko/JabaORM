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
@JabaEntity(tableName = "automobile")
public class Car {

    @JabaColumn
    private String brand;

    @JabaColumn(columnName = "manufacture_name")
    private Date manufactureYear;

    @JabaColumn
    private Integer power;

    @JabaColumn
    private String colour;

    public Car() {
    }
}
