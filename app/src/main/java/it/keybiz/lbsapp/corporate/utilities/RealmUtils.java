/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import io.realm.ImportFlag;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmMigrationNeededException;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.models.HLUser;

/**
 * Class containing various utility methods concerning {@link Realm} operations.
 *
 * @author mbaldrighi on 10/7/2017.
 */
public class RealmUtils {

	public static final String LOG_TAG = RealmUtils.class.getCanonicalName();

	/**
	 * Instantiate a {@link Realm} with default {@link io.realm.RealmConfiguration} as defined by
	 * {@link LBSLinkApp#realmConfig}.
	 * @return A default {@link Realm} instance.
	 */
	public static Realm getCheckedRealm() {
		Realm realm = null;

		try {
			realm = Realm.getDefaultInstance();
		} catch (RealmMigrationNeededException | IllegalArgumentException e){
			LogUtils.e(LOG_TAG, e.getMessage(), e);

			// TODO: 10/7/2017   something with ANALYTICS FRAMEWORK

			try {
				Realm.deleteRealm(LBSLinkApp.realmConfig);
				realm = Realm.getDefaultInstance();
			} catch (Exception ex){
				LogUtils.e(LOG_TAG, e.getMessage(), ex);
			}
		}

		LBSLinkApp.openRealms.add(realm);

		return realm;
	}

	public static Realm checkAndFetchRealm(Realm realm) {
		if (!isValid(realm))
			return getCheckedRealm();
		return realm;
	}

	public static HLUser checkAndFetchUser(Realm realm, HLUser user) {
		if (!RealmObject.isValid(user))
			user = new HLUser().readUser(realm);

		Utils.logUserForCrashlytics(user);

		return user;
	}

	public static void closeRealm(Realm realm) {
		if (realm != null) {
			realm.close();
			LBSLinkApp.openRealms.remove(realm);
		}
	}

	public static void closeAllRealms() {
		if (LBSLinkApp.openRealms != null && !LBSLinkApp.openRealms.isEmpty()) {
			for (Realm realm : LBSLinkApp.openRealms) {
				try {
					if (isValid(realm)) realm.close();
				} catch (Exception e) {
					e.printStackTrace();
					LogUtils.e(LOG_TAG, "Close All Realms ERROR");
				}
			}

			LBSLinkApp.openRealms.clear();
		}
	}

	/**
	 * Checks whether the provided {@link Realm} instance is valid.
	 * @param realm the provided {@link Realm} instance.
	 * @return True if the instance is still valid, false otherwise.
	 */
	public static boolean isValid(Realm realm) {
		return realm != null && !realm.isClosed();
	}

	/**
	 * Checks if at least one object is present for a given {@link RealmModel}.
	 * @param model the given {@link RealmModel}.
	 * @return True if at least one object is present, false otherwise.
	 */
	public static boolean hasTableObject(Realm realm, Class<? extends RealmModel> model) {
		RealmResults<RealmModel> results = readFromRealm(realm, model);
		return results != null && !results.isEmpty();
	}

	/**
	 * Retrieves all the objects for a given {@link RealmModel}.
	 * @param model the given {@link RealmModel}.
	 * @return The {@link RealmResults} containing all the wanted objects.
	 */
	public static RealmResults<RealmModel> readFromRealm(Realm realm, Class<? extends RealmModel> model) {
		if (isValid(realm)) {
			return readFromRealmWithIdSorted(realm, model,null, null, (String[]) null, null);
		}

		return null;
	}

	/**
	 * Retrieves all the objects for a given {@link RealmModel}.
	 * @param model the given {@link RealmModel}.
	 * @return The {@link RealmResults} containing all the wanted objects.
	 */
	public static RealmResults<RealmModel> readFromRealmSorted(Realm realm,
	                                                           Class<? extends RealmModel> model,
	                                                           @NonNull String sortFieldName,
	                                                           @NonNull Sort sortOrder) {
		if (isValid(realm)) {
			return readFromRealmWithIdSorted(realm, model,null, null, sortFieldName, sortOrder);
		}

		return null;
	}

	/**
	 * Retrieves only the first object for a given {@link RealmModel} and containing a given "_id".
	 * @param realm the {@link Realm} used to operate the query.
	 * @param model the given {@link RealmModel}.
	 * @param id the given id string.
	 * @return The wanted {@link RealmModel} instance if present, null otherwise.
	 */
	public static RealmResults<RealmModel> readFromRealmWithId(Realm realm, Class<? extends RealmModel> model, @Nullable String fieldName, @NonNull String id) {
		if (isValid(realm))
			return readFromRealmWithIdSorted(realm, model, fieldName, id, (String[]) null, null);

		return null;
	}

	/**
	 * Retrieves only the first object for a given {@link RealmModel} and containing a given "_id".
	 * @param realm the {@link Realm} used to operate the query.
	 * @param model the given {@link RealmModel}.
	 * @param fieldName the given id string.
	 * @param id the given id string.
	 * @param sortFieldName the given string carrying the name of the field on which perform sorting ops.
	 * @param sortOrder the given {@link Sort} value carrying the sort order for the field name.
	 * @return The wanted {@link RealmModel} instance if present, null otherwise.
	 */
	public static RealmResults<RealmModel> readFromRealmWithIdSorted(Realm realm,
	                                                                 Class<? extends RealmModel> model,
	                                                                 @Nullable String fieldName,
	                                                                 @Nullable String id,
	                                                                 @NonNull String sortFieldName,
	                                                                 @NonNull Sort sortOrder) {
		if (isValid(realm))
			return readFromRealmWithIdSorted(realm, model, fieldName, id, new String[]{sortFieldName}, new Sort[]{sortOrder});

		return null;
	}

	/**
	 * Retrieves only the first object for a given {@link RealmModel} and containing a given "_id".
	 * @param realm the {@link Realm} used to operate the query.
	 * @param model the given {@link RealmModel}.
	 * @param fieldName the given id string.
	 * @param id the given id string.
	 * @param fieldNames the given string array containing the fields on which perform sorting ops.
	 * @param sortOrders the given {@link Sort} array containing the sort orders for each sorting field name.
	 * @return The wanted {@link RealmModel} instance if present, null otherwise.
	 */
	public static RealmResults<RealmModel> readFromRealmWithIdSorted(Realm realm,
	                                                                 Class<? extends RealmModel> model,
	                                                                 @Nullable String fieldName,
	                                                                 @Nullable String id,
	                                                                 @Nullable String[] fieldNames,
	                                                                 @Nullable Sort[] sortOrders) {
		if (isValid(realm)) {
			RealmQuery<? extends RealmModel> query = realm.where(model);
			if (Utils.isStringValid(id))
				query.equalTo(Utils.isStringValid(fieldName) ? fieldName : "id", id);
			if (fieldNames != null && fieldNames.length > 0 &&
					sortOrders != null && sortOrders.length > 0)
				query.sort(fieldNames, sortOrders);
			return (RealmResults<RealmModel>) query.findAll();
		}

		return null;
	}

	/**
	 * Retrieves only the first object for a given {@link RealmModel}.
	 * @param model the given {@link RealmModel}.
	 * @return The wanted {@link RealmModel} instance if present, null otherwise.
	 */
	public static RealmModel readFirstFromRealm(Realm realm, Class<? extends RealmModel> model) {
		if (isValid(realm))
			return realm.where(model).findFirst();

		return null;
	}

	/**
	 * Retrieves only the first object for a given {@link RealmModel} and containing a given "_id".
	 * @param realm the {@link Realm} used to operate the query.
	 * @param model the given {@link RealmModel}.
	 * @param id the given id string.
	 * @return The wanted {@link RealmModel} instance if present, null otherwise.
	 */
	public static RealmModel readFirstFromRealmWithId(Realm realm, Class<? extends RealmModel> model, @Nullable String fieldName, @NonNull String id) {
		if (isValid(realm))
			return realm.where(model).equalTo(Utils.isStringValid(fieldName) ? fieldName : "id", id).findFirst();

		return null;
	}

	/**
	 * Writes given {@link RealmModel} to {@link Realm} through the action of
	 * {@link Realm#copyToRealmOrUpdate(RealmModel, ImportFlag...)} method.
	 * @param model the given {@link RealmModel}.
	 */
	public static void writeToRealm(Realm realm, final RealmModel model) {
		if (model != null && isValid(realm) ) {
			realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(model));
		}
	}

	/**
	 * Writes given {@link RealmModel} to {@link Realm} through the action of
	 * {@link Realm#copyToRealmOrUpdate(RealmModel, ImportFlag...)} method.
	 * @param model the given {@link RealmModel}.
	 */
	public static void writeToRealmNoTransaction(Realm realm, final RealmModel model) {
		if (model != null && isValid(realm) ) {
			realm.copyToRealmOrUpdate(model);
		}
	}

	/**
	 * Writes given {@link RealmModel} to {@link Realm} through the action of
	 * {@link Realm#copyToRealmOrUpdate(RealmModel, ImportFlag...)} method.
	 * @param model the given {@link RealmModel}.
	 * @param json the given {@link JSONObject}.
	 */
	public static void writeToRealmFromJson(Realm realm, final Class<? extends RealmModel> model, final JSONObject json) {
		if (json != null && isValid(realm) ) {
			realm.executeTransaction(realm1 -> realm1.createOrUpdateObjectFromJson(model, json));
		}
	}

	/**
	 * Deletes all the entries for the provided {@link RealmModel}.
	 * @param realm the {@link Realm} instance to be used.
	 * @param model the provided {@link RealmModel}.
	 */
	public static void deleteTable(Realm realm, Class<? extends RealmModel> model) {
		if (isValid(realm) && model != null)
			realm.delete(model);
	}

}
