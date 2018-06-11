package com.chupapp.distributor.database;

/**
 * Created by inmobitec on 27/11/17.
 */
import android.provider.BaseColumns;

public final class Contract {

    private Contract() {
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "chupapp";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_DISTRIBUTOR = "distributor"; //json
        public static final String COLUMN_ORDERS = "orders"; //ordenes de los usuarios
        public static final String COLUMN_STATUS = "status"; //string
    }

    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + Entry.TABLE_NAME + " (" +
            Entry.COLUMN_ID + " INTEGER PRIMARY KEY," +
            Entry.COLUMN_DISTRIBUTOR + " TEXT, " +
            Entry.COLUMN_ORDERS+ " TEXT," +
            Entry.COLUMN_STATUS + " TEXT" +
            ")";

    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + Entry.TABLE_NAME;

}
