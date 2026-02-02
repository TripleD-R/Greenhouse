package com.example.test.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SensorDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sensor_data.db";
    private static final int DATABASE_VERSION = 2;

    // Таблицы
    public static final String TABLE_SENSOR_TYPES = "sensor_types";
    public static final String TABLE_SENSORS = "sensors";
    public static final String TABLE_HISTORY = "sensor_history";

    public SensorDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ===================== СОЗДАНИЕ БД =====================

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Типы сенсоров
        db.execSQL(
                "CREATE TABLE " + TABLE_SENSOR_TYPES + " (" +
                        "type_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type_name TEXT NOT NULL UNIQUE" +
                        ");"
        );

        // Сенсоры
        db.execSQL(
                "CREATE TABLE " + TABLE_SENSORS + " (" +
                        "sensor_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "type_id INTEGER NOT NULL," +
                        "location TEXT," +
                        "FOREIGN KEY(type_id) REFERENCES " + TABLE_SENSOR_TYPES + "(type_id)" +
                        ");"
        );

        // История измерений
        db.execSQL(
                "CREATE TABLE " + TABLE_HISTORY + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "sensor_id INTEGER NOT NULL," +
                        "timestamp INTEGER NOT NULL," +
                        "value REAL NOT NULL," +
                        "FOREIGN KEY(sensor_id) REFERENCES " + TABLE_SENSORS + "(sensor_id)" +
                        ");"
        );

        // Начальные типы сенсоров
        db.execSQL("INSERT INTO " + TABLE_SENSOR_TYPES + " (type_name) VALUES ('Temperature');");
        db.execSQL("INSERT INTO " + TABLE_SENSOR_TYPES + " (type_name) VALUES ('Humidity');");
        db.execSQL("INSERT INTO " + TABLE_SENSOR_TYPES + " (type_name) VALUES ('Light');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSORS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_TYPES);
        onCreate(db);
    }

    // ===================== ОСНОВНОЙ МЕТОД =====================

    // Вызывать после получения JSON
    public void saveSensorData(SensorData data) {
        SQLiteDatabase db = getWritableDatabase();

        ensureSensors(db);

        long timestamp = System.currentTimeMillis();

        insertHistory(db, "Temperature", data.getTemperature(), timestamp);
        insertHistory(db, "Humidity", data.getHumidity(), timestamp);
        insertHistory(db, "Light", data.getLight(), timestamp);
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    // Создаёт сенсоры, если их ещё нет
    private void ensureSensors(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SENSORS, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        if (count > 0) return;

        db.execSQL(
                "INSERT INTO " + TABLE_SENSORS + " (name, type_id, location) VALUES " +
                        "('TempSensor', 1, 'Main device')," +
                        "('HumSensor', 2, 'Main device')," +
                        "('LightSensor', 3, 'Main device')"
        );
    }

    // Запись измерений в историю
    private void insertHistory(SQLiteDatabase db, String typeName, float value, long timestamp) {

        String query =
                "SELECT s.sensor_id FROM " + TABLE_SENSORS + " s " +
                        "JOIN " + TABLE_SENSOR_TYPES + " t ON s.type_id = t.type_id " +
                        "WHERE t.type_name = ?";

        Cursor cursor = db.rawQuery(query, new String[]{typeName});

        if (cursor.moveToFirst()) {
            int sensorId = cursor.getInt(0);

            ContentValues cv = new ContentValues();
            cv.put("sensor_id", sensorId);
            cv.put("timestamp", timestamp);
            cv.put("value", value);

            db.insert(TABLE_HISTORY, null, cv);
        }

        cursor.close();
    }
}
