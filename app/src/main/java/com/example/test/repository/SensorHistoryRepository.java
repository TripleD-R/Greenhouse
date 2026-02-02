package com.example.test.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.test.model.SensorDatabaseHelper;
import com.example.test.model.SensorData;

import java.util.ArrayList;
import java.util.List;

public class SensorHistoryRepository {

    private final SensorDatabaseHelper dbHelper;

    public SensorHistoryRepository(Context context) {
        dbHelper = new SensorDatabaseHelper(context);
    }

    // ====================== Вставка данных ======================
    public void insert(SensorData data) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long timestamp = System.currentTimeMillis();

        insertSingle(db, "Temperature", data.getTemperature(), timestamp);
        insertSingle(db, "Humidity", data.getHumidity(), timestamp);
        insertSingle(db, "Light", data.getLight(), timestamp);

        db.close();
    }

    private void insertSingle(SQLiteDatabase db, String typeName, float value, long timestamp) {
        Cursor c = db.rawQuery(
                "SELECT s.sensor_id FROM " + SensorDatabaseHelper.TABLE_SENSORS + " s " +
                        "JOIN " + SensorDatabaseHelper.TABLE_SENSOR_TYPES + " t ON s.type_id = t.type_id " +
                        "WHERE t.type_name = ?",
                new String[]{typeName}
        );

        if (c.moveToFirst()) {
            int sensorId = c.getInt(0);

            ContentValues cv = new ContentValues();
            cv.put("sensor_id", sensorId);
            cv.put("timestamp", timestamp);
            cv.put("value", value);

            db.insert(SensorDatabaseHelper.TABLE_HISTORY, null, cv);
        }

        c.close();
    }

    // ====================== Получение последних N записей ======================
    public List<SensorData> getLastN(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT h.timestamp, " +
                        "MAX(CASE WHEN t.type_name='Temperature' THEN h.value END) AS temperature, " +
                        "MAX(CASE WHEN t.type_name='Humidity' THEN h.value END) AS humidity, " +
                        "MAX(CASE WHEN t.type_name='Light' THEN h.value END) AS light " +
                        "FROM " + SensorDatabaseHelper.TABLE_HISTORY + " h " +
                        "JOIN " + SensorDatabaseHelper.TABLE_SENSORS + " s ON h.sensor_id = s.sensor_id " +
                        "JOIN " + SensorDatabaseHelper.TABLE_SENSOR_TYPES + " t ON s.type_id = t.type_id " +
                        "GROUP BY h.timestamp " +
                        "ORDER BY h.timestamp DESC " +
                        "LIMIT ?";

        Cursor c = db.rawQuery(query, new String[]{String.valueOf(limit)});
        List<SensorData> list = new ArrayList<>();

        while (c.moveToNext()) {
            list.add(new SensorData(
                    c.getFloat(c.getColumnIndexOrThrow("temperature")),
                    c.getFloat(c.getColumnIndexOrThrow("humidity")),
                    c.getFloat(c.getColumnIndexOrThrow("light"))
            ));
        }

        c.close();
        db.close();
        return list;
    }
}
