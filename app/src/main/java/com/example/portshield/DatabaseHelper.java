package com.example.portshield;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite persistence helper implementing full CRUD operations on the
 * security_events table using parameterized queries and ContentValues.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "PortShieldDbHelper";
    private static final String DATABASE_NAME = "portshield_events.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_EVENTS = "security_events";
    public static final String COL_ID = "id";
    public static final String COL_EVENT_TYPE = "event_type";
    public static final String COL_RISK_SCORE = "risk_score";
    public static final String COL_RISK_LEVEL = "risk_level";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_EVIDENCE_JSON = "evidence_json";
    public static final String COL_EVENT_SOURCE = "event_source";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_REVIEW_STATUS = "review_status";
    public static final String COL_NOTES = "notes";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_EVENTS + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_EVENT_TYPE + " TEXT NOT NULL, "
                    + COL_RISK_SCORE + " INTEGER NOT NULL, "
                    + COL_RISK_LEVEL + " TEXT NOT NULL, "
                    + COL_DESCRIPTION + " TEXT, "
                    + COL_EVIDENCE_JSON + " TEXT, "
                    + COL_EVENT_SOURCE + " TEXT NOT NULL, "
                    + COL_TIMESTAMP + " INTEGER NOT NULL, "
                    + COL_REVIEW_STATUS + " TEXT NOT NULL, "
                    + COL_NOTES + " TEXT"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    /**
     * CREATE: Inserts a new SecurityEvent into security_events and returns its row ID.
     */
    public long insertEvent(SecurityEvent event) {
        if (event == null) return -1;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_EVENT_TYPE, event.getEventType());
            values.put(COL_RISK_SCORE, event.getRiskScore());
            values.put(COL_RISK_LEVEL, event.getRiskLevel());
            values.put(COL_DESCRIPTION, event.getDescription());
            values.put(COL_EVIDENCE_JSON, event.toEvidenceJsonString());
            values.put(COL_EVENT_SOURCE, event.getEventSource());
            values.put(COL_TIMESTAMP, event.getTimestamp());
            values.put(COL_REVIEW_STATUS, event.getReviewStatus());
            values.put(COL_NOTES, event.getNotes());

            long id = db.insert(TABLE_EVENTS, null, values);
            event.setId(id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting security event", e);
            return -1;
        }
    }

    /**
     * READ: Retrieves all security events ordered newest first (timestamp DESC, id DESC).
     * Returns an empty list if no records exist.
     */
    public List<SecurityEvent> getAllEventsNewestFirst() {
        List<SecurityEvent> events = new ArrayList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC, " + COL_ID + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    events.add(cursorToEvent(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading all security events", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }

    /**
     * READ: Retrieves the single newest event for the Home Dashboard, or null if empty.
     */
    public SecurityEvent getLatestEvent() {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_TIMESTAMP + " DESC, " + COL_ID + " DESC",
                    "1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEvent(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching latest event", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * READ: Retrieves a single SecurityEvent by its primary key ID.
     */
    public SecurityEvent getEventById(long id) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(
                    TABLE_EVENTS,
                    null,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEvent(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching event by ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * UPDATE: Updates only the review_status and notes columns for an existing record.
     * Strictly preserves original score, risk_level, description, evidence_json, and source.
     */
    public boolean updateReviewAndNotes(long id, String reviewStatus, String notes) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_REVIEW_STATUS, reviewStatus != null ? reviewStatus : SecurityEvent.REVIEW_PENDING);
            values.put(COL_NOTES, notes != null ? notes : "");

            int rows = db.update(
                    TABLE_EVENTS,
                    values,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating event ID: " + id, e);
            return false;
        }
    }

    /**
     * DELETE: Deletes a single security event by ID.
     */
    public boolean deleteEventById(long id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rows = db.delete(
                    TABLE_EVENTS,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(id)}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting event ID: " + id, e);
            return false;
        }
    }

    /**
     * DELETE: Clears all security events from the table.
     */
    public int clearAllEvents() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            return db.delete(TABLE_EVENTS, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all events", e);
            return 0;
        }
    }

    private SecurityEvent cursorToEvent(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        String eventType = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TYPE));
        int riskScore = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RISK_SCORE));
        String riskLevel = cursor.getString(cursor.getColumnIndexOrThrow(COL_RISK_LEVEL));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION));
        String evidenceJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVIDENCE_JSON));
        String eventSource = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_SOURCE));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
        String reviewStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_STATUS));
        String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES));

        SecurityEvent.EvidencePayload payload = SecurityEvent.parseEvidenceJson(evidenceJson);

        return new SecurityEvent(
                id,
                eventType,
                riskScore,
                riskLevel,
                description,
                payload.evidenceList,
                payload.recommendedAction,
                payload.confidenceIndicator,
                eventSource,
                timestamp,
                reviewStatus,
                notes
        );
    }
}