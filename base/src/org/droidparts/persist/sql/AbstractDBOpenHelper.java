/**
 * Copyright 2012 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.droidparts.persist.sql;

import static org.droidparts.reflect.util.TypeHelper.isArray;
import static org.droidparts.reflect.util.TypeHelper.isBitmap;
import static org.droidparts.reflect.util.TypeHelper.isBoolean;
import static org.droidparts.reflect.util.TypeHelper.isByteArray;
import static org.droidparts.reflect.util.TypeHelper.isCollection;
import static org.droidparts.reflect.util.TypeHelper.isDouble;
import static org.droidparts.reflect.util.TypeHelper.isEntity;
import static org.droidparts.reflect.util.TypeHelper.isEnum;
import static org.droidparts.reflect.util.TypeHelper.isFloat;
import static org.droidparts.reflect.util.TypeHelper.isInteger;
import static org.droidparts.reflect.util.TypeHelper.isLong;
import static org.droidparts.reflect.util.TypeHelper.isString;
import static org.droidparts.reflect.util.TypeHelper.isUUID;
import static org.droidparts.util.Strings.join;

import java.util.ArrayList;

import org.droidparts.contract.DB.Column;
import org.droidparts.contract.SQL;
import org.droidparts.model.Entity;
import org.droidparts.reflect.model.EntityField;
import org.droidparts.reflect.processor.EntityAnnotationProcessor;
import org.droidparts.util.L;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class AbstractDBOpenHelper extends SQLiteOpenHelper implements
		SQL.DDL {

	public AbstractDBOpenHelper(Context ctx, String name, int version) {
		super(ctx.getApplicationContext(), name, null, version);
	}

	@Override
	public final void onCreate(SQLiteDatabase db) {
		ArrayList<String> queries = new ArrayList<String>();
		for (Class<? extends Entity> cls : getModelClasses()) {
			String query = getSQLCreate(new EntityAnnotationProcessor(cls));
			queries.add(query);
		}
		execQueries(db, queries);
		onCreateExtra(db);
	}

	protected void onCreateExtra(SQLiteDatabase db) {
	}

	public static void execQueries(SQLiteDatabase db, ArrayList<String> queries) {
		db.beginTransaction();
		try {
			for (String query : queries) {
				L.d(query);
				db.execSQL(query);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	protected static void createIndex(SQLiteDatabase db, String table,
			boolean unique, String... columns) {
		StringBuilder sb = new StringBuilder();
		sb.append(unique ? CREATE_UNIQUE_INDEX : CREATE_INDEX);
		sb.append("idx_" + table + "_" + join(columns, "_", null));
		sb.append(ON + table);
		sb.append(OPENING_BRACE);
		sb.append(join(columns, SEPARATOR, null));
		sb.append(CLOSING_BRACE);
		db.execSQL(sb.toString());
	}

	protected void dropAll(SQLiteDatabase db, boolean tables, boolean indexes) {
		ArrayList<String> queries = new ArrayList<String>();
		for (Class<? extends Entity> cls : getModelClasses()) {
			String tableName = new EntityAnnotationProcessor(cls)
					.getModelClassName();
			if (tables) {
				queries.add("DROP TABLE IF EXISTS " + tableName + ";");
			}
			if (indexes) {
				// TODO
				// SELECT name FROM sqlite_master WHERE type='index'
				// if (index.startsWith("idx_" + tableName)
				// queries.add("DROP INDEX IF EXISTS " + indexName + ";");
			}
		}
		execQueries(db, queries);
	}

	protected abstract Class<? extends Entity>[] getModelClasses();

	private String getSQLCreate(EntityAnnotationProcessor proc) {
		EntityField[] dbFields = proc.getModelClassFields();
		StringBuilder sb = new StringBuilder();
		sb.append(CREATE_TABLE + proc.getModelClassName() + OPENING_BRACE);
		sb.append(PK);
		for (int i = 0; i < dbFields.length; i++) {
			EntityField dbField = dbFields[i];
			if (Column.ID.equals(dbField.columnName)) {
				// already got it
				continue;
			}
			String columnType = getColumnType(dbField.fieldClass);
			sb.append(SEPARATOR);
			sb.append(dbField.columnName);
			sb.append(" ");
			sb.append(columnType);
			if (!dbField.columnNullable) {
				sb.append(" ");
				sb.append(NOT_NULL);
			}
			if (dbField.columnUnique) {
				sb.append(" ");
				sb.append(UNIQUE);
			}
		}
		sb.append(CLOSING_BRACE);
		return sb.toString();
	}

	private String getColumnType(Class<?> cls) {
		if (isBoolean(cls) || isInteger(cls) || isLong(cls)) {
			return INTEGER;
		} else if (isFloat(cls) || isDouble(cls)) {
			return REAL;
		} else if (isString(cls) || isUUID(cls) || isEnum(cls)) {
			return TEXT;
		} else if (isByteArray(cls) || isBitmap(cls)) {
			return BLOB;
		} else if (isArray(cls) || isCollection(cls)) {
			return TEXT;
		} else if (isEntity(cls)) {
			return INTEGER;
		} else {
			// persist any other type as blob
			return BLOB;
		}
	}

}
