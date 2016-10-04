package org.autorefactor.refactoring.rules.samples_in;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;

public class RecycleSample {
    public void cursorError1(SQLiteDatabase db, long route_id) {
        Cursor cursor = db.query("TABLE_TRIPS",
                new String[]{"KEY_TRIP_ID"},
                "ROUTE_ID=?",
                new String[]{Long.toString(route_id)},
                null, null, null);
    }
    
    void testProviderQueries(Uri uri, ContentProvider provider, ContentResolver resolver,
    		ContentProviderClient client) throws RemoteException {
    	Cursor query = provider.query(uri, null, null, null, null);
    	Cursor query2 = resolver.query(uri, null, null, null, null);
    	Cursor query3 = client.query(uri, null, null, null, null);
    }
    
    public int ok(SQLiteDatabase db, long route_id, String table, String whereClause, String id) {
        int total_deletions = 0;
        Cursor cursor = db.query("TABLE_TRIPS",
                new String[]{
                        "KEY_TRIP_ID"},
                "ROUTE_ID" + "=?",
                new String[]{Long.toString(route_id)},
                null, null, null);

        while (cursor.moveToNext()) {
            total_deletions += db.delete(table, whereClause + "=?",
                    new String[]{Long.toString(cursor.getLong(0))});
        }

        return total_deletions;
    }
}

