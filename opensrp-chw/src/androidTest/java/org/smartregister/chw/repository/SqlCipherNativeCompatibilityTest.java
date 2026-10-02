package org.smartregister.chw.repository;

import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises the packaged native library; run on a 16 KB emulator as well as normal devices. */
@RunWith(AndroidJUnit4.class)
public class SqlCipherNativeCompatibilityTest {
    @Test
    public void encryptedDatabaseCanWriteAndReopen() throws Exception {
        System.loadLibrary("sqlcipher");
        File file = File.createTempFile("sqlcipher-native-", ".db",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        String key = UUID.randomUUID().toString();
        try {
            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, key, null, null);
            try {
                try (Cursor version = database.rawQuery("PRAGMA cipher_version", null)) {
                    assertTrue(version.moveToFirst());
                    assertFalse(version.getString(0).isEmpty());
                }
                database.execSQL("CREATE TABLE probe (id INTEGER PRIMARY KEY, value TEXT)");
                database.execSQL("INSERT INTO probe VALUES (1, 'before')");
                database.execSQL("UPDATE probe SET value = 'after' WHERE id = 1");
            } finally {
                database.close();
            }

            database = SQLiteDatabase.openOrCreateDatabase(file, key, null, null);
            try {
                try (Cursor row = database.rawQuery("SELECT value FROM probe WHERE id = 1", null)) {
                    assertTrue(row.moveToFirst());
                    assertEquals("after", row.getString(0));
                }
                try (Cursor integrity = database.rawQuery("PRAGMA integrity_check", null)) {
                    assertTrue(integrity.moveToFirst());
                    assertEquals("ok", integrity.getString(0));
                }
                database.execSQL("DELETE FROM probe WHERE id = 1");
                try (Cursor count = database.rawQuery("SELECT count(*) FROM probe", null)) {
                    assertTrue(count.moveToFirst());
                    assertEquals(0, count.getInt(0));
                }
            } finally {
                database.close();
            }

            byte[] header = new byte[16];
            try (FileInputStream stream = new FileInputStream(file)) {
                assertEquals(header.length, stream.read(header));
            }
            assertFalse(new String(header, StandardCharsets.US_ASCII).startsWith("SQLite format 3"));
        } finally {
            SQLiteDatabase.deleteDatabase(file);
        }
    }
}
