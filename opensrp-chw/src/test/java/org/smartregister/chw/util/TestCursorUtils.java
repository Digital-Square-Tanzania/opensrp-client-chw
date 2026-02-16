package org.smartregister.chw.util;

import android.database.Cursor;

import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestCursorUtils {

    private TestCursorUtils() {
    }

    public static Cursor mockCursor(String[] columns, Object[]... rows) {
        Cursor cursor = Mockito.mock(Cursor.class, Mockito.withSettings().lenient());
        AtomicInteger position = new AtomicInteger(-1);
        Map<String, Integer> columnIndexes = new HashMap<>();

        for (int i = 0; i < columns.length; i++) {
            columnIndexes.put(columns[i], i);
        }

        Mockito.doAnswer(invocation -> {
            int nextPosition = position.get() + 1;
            if (nextPosition < rows.length) {
                position.set(nextPosition);
                return true;
            }
            position.set(rows.length);
            return false;
        }).when(cursor).moveToNext();

        Mockito.doAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            Integer index = columnIndexes.get(columnName);
            return index == null ? -1 : index;
        }).when(cursor).getColumnIndex(Mockito.anyString());

        Mockito.doAnswer(invocation -> {
            int columnIndex = invocation.getArgument(0);
            Object value = getValue(rows, position.get(), columnIndex);
            if (value == null) {
                return Cursor.FIELD_TYPE_NULL;
            }
            if (value instanceof Float || value instanceof Double) {
                return Cursor.FIELD_TYPE_FLOAT;
            }
            if (value instanceof Number || value instanceof Boolean) {
                return Cursor.FIELD_TYPE_INTEGER;
            }
            return Cursor.FIELD_TYPE_STRING;
        }).when(cursor).getType(Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            int columnIndex = invocation.getArgument(0);
            Object value = getValue(rows, position.get(), columnIndex);
            return value == null ? null : String.valueOf(value);
        }).when(cursor).getString(Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            int columnIndex = invocation.getArgument(0);
            Object value = getValue(rows, position.get(), columnIndex);
            if (value == null) {
                return 0;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            return Integer.parseInt(String.valueOf(value));
        }).when(cursor).getInt(Mockito.anyInt());

        Mockito.doAnswer(invocation -> {
            int columnIndex = invocation.getArgument(0);
            Object value = getValue(rows, position.get(), columnIndex);
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1L : 0L;
            }
            return Long.parseLong(String.valueOf(value));
        }).when(cursor).getLong(Mockito.anyInt());

        Mockito.doReturn(rows.length).when(cursor).getCount();
        Mockito.doReturn(columns.length).when(cursor).getColumnCount();
        Mockito.doAnswer(invocation -> columns[invocation.getArgument(0)]).when(cursor).getColumnName(Mockito.anyInt());

        return cursor;
    }

    private static Object getValue(Object[][] rows, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.length) {
            return null;
        }
        if (columnIndex < 0 || columnIndex >= rows[rowIndex].length) {
            return null;
        }
        return rows[rowIndex][columnIndex];
    }
}
