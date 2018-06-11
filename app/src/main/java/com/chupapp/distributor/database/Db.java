package com.chupapp.distributor.database;

/**
 * Created by inmobitec on 27/11/17.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Db {
    private final DbHelper dbh;
    private final SQLiteDatabase db_write,db_read;

    Context contexto;

    public Db(Context context) {
        contexto = context;
        dbh = new DbHelper(context);
        db_write = dbh.getWritableDatabase();
        db_read = dbh.getReadableDatabase();
    }

    public JSONObject init(){

        JSONObject data = read();

        //Log.v("INICIANDO", "length: "+data.length());

        try {

            //Si no existe creo
            if (data.length()<=0){

                ContentValues values = new ContentValues();
                //values.put(Contract.Entry.COLUMN_ID, "");
                values.put(Contract.Entry.COLUMN_DISTRIBUTOR, "");
                values.put(Contract.Entry.COLUMN_ORDERS, "");
                values.put(Contract.Entry.COLUMN_STATUS, "");

                data = create(values);


                Log.v("INICIANDO", "CREADO: "+data.toString(1));
            }else{
                Log.v("INICIANDO", "OBTENIDO: "+data.toString(1));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return data;
    }

    public JSONObject create(ContentValues values) throws JSONException {
        db_write.insert(Contract.Entry.TABLE_NAME, null, values);
        return read();
    }

    public JSONObject read(){
        String[] projection = {
                Contract.Entry.COLUMN_ID,
                Contract.Entry.COLUMN_DISTRIBUTOR,
                Contract.Entry.COLUMN_ORDERS,
                Contract.Entry.COLUMN_STATUS
        };

        String selection = Contract.Entry.COLUMN_ID + " = ?";
        String[] selectionArgs = { "1" };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = Contract.Entry.COLUMN_ID + " DESC";

        Cursor c = db_read.query(
                Contract.Entry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return getData(c);
    }

    public void delete(){
        String selection = Contract.Entry.COLUMN_ID + " = ?";
        String[] selectionArgs = { "1" };

        db_write.delete(Contract.Entry.TABLE_NAME, selection, selectionArgs);
    }

    public JSONObject update(ContentValues values){
        //Log.v("LOGIN dbupdate",uloveContract.uloveEntry.COLUMN_ID);
        db_read.update(Contract.Entry.TABLE_NAME, values, Contract.Entry.COLUMN_ID+"=1", null);

        JSONObject data = read();
        return data;
    }

    public JSONObject getData(Cursor cursor){
        //ArrayList list = new ArrayList();
        JSONObject values = new JSONObject();

        //Log.v("GETDATA","PASO");

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            //Log.v("GETDATA",cursor.toString());
            values = setCursor(cursor);
        }

        cursor.close();

        return values;
    }

    public JSONObject setCursor(Cursor cursor){
        JSONObject c = new JSONObject();
        //Log.v("GETDATA","PASO - - -");

        try {
            c.put(Contract.Entry.COLUMN_ID, cursor.getString(0));
            c.put(Contract.Entry.COLUMN_DISTRIBUTOR, cursor.getString(1));
            c.put(Contract.Entry.COLUMN_ORDERS, cursor.getString(2));
            c.put(Contract.Entry.COLUMN_STATUS, cursor.getString(3));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return c;
    }
}
