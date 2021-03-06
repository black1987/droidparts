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
package org.droidparts.adapter.cursor;

import org.droidparts.model.Entity;
import org.droidparts.persist.sql.EntityManager;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;

public abstract class EntityCursorAdapter<EntityType extends Entity> extends
		CursorAdapter<EntityType> {

	protected final EntityManager<EntityType> entityManager;

	public EntityCursorAdapter(Activity activity,
			EntityManager<EntityType> entityManager) {
		this(activity, entityManager, entityManager.list());
	}

	public EntityCursorAdapter(Activity activity,
			EntityManager<EntityType> entityManager, Cursor cursor) {
		super(activity, cursor);
		this.entityManager = entityManager;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		bindView(context, view, entityManager.readFromCursor(cursor));
	}

	public abstract void bindView(Context context, View view, EntityType item);

	public boolean create(EntityType item) {
		boolean success = entityManager.create(item);
		return requeryOnSuccess(success);
	}

	public EntityType read(int position) {
		long id = getItemId(position);
		EntityType item = entityManager.read(id);
		String[] eagerFieldNames = entityManager.getEagerForeignKeyFieldNames();
		if (eagerFieldNames.length != 0) {
			entityManager.fillForeignKeys(item, eagerFieldNames);
		}
		return item;
	}

	public boolean update(EntityType item) {
		boolean success = entityManager.update(item);
		return requeryOnSuccess(success);
	}

	public boolean delete(int position) {
		long id = getItemId(position);
		boolean success = entityManager.delete(id);
		return requeryOnSuccess(success);
	}

	private boolean requeryOnSuccess(boolean success) {
		if (success) {
			getCursor().requery();
		}
		return success;
	}

}
