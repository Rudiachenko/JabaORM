package cdp.jabaorm.test.model;

import cdp.jabaorm.annotations.JabaColumn;
import cdp.jabaorm.annotations.JabaEntity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@JabaEntity(tableName = "employee")
public class Employee {
    @JabaColumn
    private String name;
    @JabaColumn
    private String surName;
    @JabaColumn
    private String position;
    @JabaColumn(columnName = "experience")
    private Integer experienceInYears;

    public Employee() {
    }
}
