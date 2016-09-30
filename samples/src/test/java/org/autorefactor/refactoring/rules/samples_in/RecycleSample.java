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
}

