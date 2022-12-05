package cdp.jabaorm;

import cdp.jabaorm.annotations.JabaColumn;
import cdp.jabaorm.annotations.JabaEntity;
import com.sun.rowset.JdbcRowSetImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;

import javax.sql.rowset.JdbcRowSet;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Support only some basic types (see supportedTypes array).
 * Use create(Class) method to construct new instance.
 * <p>
 * Use @JabaEntity and @JabaColumn to mark you classes.
 * <p>
 * objectTo...(e.g. objectToSave) is an object with set fields that are used to create filters.
 * null fields are ignored while creating where A=B constructions
 *
 * @param <T> type of JabaEntity
 */
public class JabaTemplate<T> {

    private String targetTableName;
    private Map<String, String> columnMappings;
    private final Class<T> targetClass;
    private final Connection dbConnection;

    private String insertTemplate;
    private String updateTemplate;
    private String deleteTemplate;
    private String selectTemplate;

    private JabaTemplate(Class<T> targetClass, Connection connection) {
        this.targetClass = targetClass;
        this.dbConnection = connection;
    }

    // Prepare template for target class. Scan fields and types, create mappings and templates.
    public static <E> JabaTemplate<E> create(Class<E> targetClass, Connection connection) {
        JabaTemplate<E> template = new JabaTemplate<>(targetClass, connection);

        JabaEntity entityMark = targetClass.getDeclaredAnnotation(JabaEntity.class);

        if (entityMark != null) {
            if (!entityMark.tableName().isEmpty()) {
                template.targetTableName = entityMark.tableName();
            } else {
                template.targetTableName = targetClass.getSimpleName().toLowerCase();
            }
        } else {
            throw new RuntimeException("Class " + targetClass.getName() + " is not marked as JabaEntity");
        }

        Field[] targetClassFields = targetClass.getDeclaredFields();
        template.columnMappings = new HashMap<>(targetClassFields.length);

        for (Field field : targetClassFields) {
            JabaColumn jabaColumn = field.getAnnotation(JabaColumn.class);

            if (jabaColumn != null) {
                if (!isTypeSupported(field.getType())) {
                    throw new RuntimeException(
                            "Type " + field.getType().getName() +
                                    " is not supported (JabaEntity: " + template.targetTableName + ")"
                    );
                }

                String fieldMapping;

                if (jabaColumn.columnName().isEmpty()) {
                    fieldMapping = field.getName().toLowerCase();
                } else {
                    fieldMapping = jabaColumn.columnName();
                }

                if (template.columnMappings.containsValue(fieldMapping)) {
                    throw new RuntimeException(
                            "Column '" + fieldMapping + "' is declared at least twice in " + template.targetTableName
                    );
                }

                template.columnMappings.put(field.getName(), fieldMapping);
            }
        }

        template.insertTemplate =
                "INSERT INTO " +
                        template.targetTableName +
                        " (${fieldsToInsert}) VALUES (${valuesToInsert});";

        template.selectTemplate =
                "SELECT * FROM " +
                        template.targetTableName +
                        " WHERE ${conditions};";

        template.updateTemplate =
                "UPDATE " + template.targetTableName +
                        " SET ${updatedValues} WHERE ${conditions};";

        template.deleteTemplate =
                "DELETE FROM " + template.targetTableName +
                        " WHERE ${conditions};";


        return template;
    }

    /**
     * @param objectToGet object with criteria (null to ignore field)
     * @return first object that match the criteria
     * @throws Exception any errors
     */
    public T get(T objectToGet) throws Exception {
        String selectQuery = getSelectQuery(objectToGet);

        JdbcRowSet rowSet = new JdbcRowSetImpl(dbConnection);
        rowSet.setType(ResultSet.TYPE_SCROLL_INSENSITIVE);

        rowSet.setCommand(selectQuery);
        rowSet.execute();

        if (!rowSet.next()) {
            return null;
        }

        Object object = targetClass.getDeclaredConstructor().newInstance();

        for (String fieldName : columnMappings.keySet()) {
            Field field = object.getClass().getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(object, rowSet.getObject(columnMappings.get(fieldName)));
        }

        return targetClass.cast(object);
    }

    /**
     * @param objectToSave object to save as a new row
     * @throws Exception any errors
     */
    public void save(T objectToSave) throws Exception {
        dbConnection
                .prepareStatement(getInsertQuery(objectToSave))
                .execute();
    }

    /**
     * @param objectToUpdate object with set fields to update
     * @return number of updated rows.
     * @throws Exception any errors
     */
    public int update(T objectToUpdate, T objectWithChanges) throws Exception {
        return dbConnection
                .prepareStatement(getUpdateQuery(objectToUpdate, objectWithChanges))
                .executeUpdate();
    }

    /**
     * Delete all matched rows
     * @param objectToDelete object with criteria
     * @throws Exception any errors
     */
    public void delete(T objectToDelete) throws Exception {
        String deleteQuery = getDeleteQuery(objectToDelete);

        PreparedStatement statement = dbConnection.prepareStatement(deleteQuery);
        statement.execute();
    }

    private String getInsertQuery(T object) throws Exception {
        Map<String, String> gaps = new HashMap<>(2);
        List<Pair<String, String>> fieldsNameValues = getFieldsNameValueList(object);

        gaps.put("fieldsToInsert", fieldsNameValues
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.joining(", ")));

        gaps.put("valuesToInsert", fieldsNameValues
                .stream()
                .map(Pair::getValue)
                .collect(Collectors.joining(", ")));

        StringSubstitutor substitutor = new StringSubstitutor(gaps);
        return substitutor.replace(insertTemplate);
    }

    private String getSelectQuery(T objectToGet) throws Exception {
        Map<String, String> gaps = new HashMap<>(1);
        List<Pair<String, String>> fieldsNameValues = getFieldsNameValueList(objectToGet);

        gaps.put("conditions", fieldsNameValues
                .stream()
                .map(fv -> fv.getKey() + " = " + fv.getValue())
                .collect(Collectors.joining(" AND ")));

        StringSubstitutor substitutor = new StringSubstitutor(gaps);
        return substitutor.replace(selectTemplate);
    }

    private String getUpdateQuery(T objectToUpdate, T objectWithChanges) throws Exception {
        Map<String, String> gaps = new HashMap<>(2);
        List<Pair<String, String>> fieldsNameValues = getFieldsNameValueList(objectToUpdate);
        List<Pair<String, String>> fieldsNameValuesForUpdatedObject = getFieldsNameValueList(objectWithChanges);

        gaps.put("updatedValues", fieldsNameValuesForUpdatedObject
                .stream()
                .map(fv -> fv.getKey() + " = " + fv.getValue())
                .collect(Collectors.joining(", ")));

        gaps.put("conditions", fieldsNameValues
                .stream()
                .map(fv -> fv.getKey() + " = " + fv.getValue())
                .collect(Collectors.joining(" AND ")));

        StringSubstitutor substitutor = new StringSubstitutor(gaps);
        return substitutor.replace(updateTemplate);
    }

    private String getDeleteQuery(T objectToDelete) throws Exception {
        Map<String, String> gaps = new HashMap<>(2);
        List<Pair<String, String>> fieldsNameValues = getFieldsNameValueList(objectToDelete);

        gaps.put("conditions", fieldsNameValues
                .stream()
                .map(fv -> fv.getKey() + " = " + fv.getValue())
                .collect(Collectors.joining(" AND ")));

        StringSubstitutor substitutor = new StringSubstitutor(gaps);
        return substitutor.replace(deleteTemplate);
    }

    // Return list of fieldName -> fieldValue for sql
    private List<Pair<String, String>> getFieldsNameValueList(T object) throws Exception {
        List<Pair<String, String>> fieldsValuesList = new ArrayList<>();

        for (String fieldName : columnMappings.keySet()) {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            Object fieldValue = field.get(object);

            if (fieldValue != null) {
                fieldsValuesList.add(Pair.of(columnMappings.get(fieldName), getSqlFriendlyValueString(fieldValue)));
            }
        }

        return fieldsValuesList;
    }

    /**
     * Some non-trivial converting can be placed here
     *
     * @param object object to convert to string
     * @return converted string
     */
    private String getSqlFriendlyValueString(Object object) {
        if (object instanceof String) {
            return "'" + object + "'";
        }

        if (object instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return "'" + sdf.format(object) + "'";
        }

        return object.toString();
    }

    /**
     * Check that type of the field is supported
     *
     * @param type type to check
     * @return check result
     */
    private static boolean isTypeSupported(Class<?> type) {
        for (Class<?> supportedType : supportedTypes) {
            if (type.equals(supportedType)) {
                return true;
            }
        }

        return false;
    }

    // List of supported types
    private static final Class<?>[] supportedTypes = {
            Long.class,
            Integer.class,
            Short.class,
            Float.class,
            Double.class,
            String.class,
            Date.class,
            Boolean.class,
            long.class,
            int.class,
            short.class,
            float.class,
            double.class,
            boolean.class
    };
}
