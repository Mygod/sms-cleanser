package be.mygod.telephony.sms.cleanser

import java.util.Locale

import android.app.ProgressDialog
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.{BaseColumns, ContactsContract, Telephony}
import android.support.v14.preference.MultiSelectListPreference
import android.support.v7.preference.Preference
import be.mygod.preference.PreferenceFragmentPlus

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class SettingsFragment extends PreferenceFragmentPlus with PermissionFragment {
  private var contactList: MultiSelectListPreference = _

  private def getContactGroups = {
    val rawSet = getPreferenceManager.getSharedPreferences.getStringSet(Constants.KEY_DELETE_CONTACT_GROUP, null)
    if (rawSet == null) mutable.Set[Int](Constants.STRANGERS) else rawSet.asScala.map(Integer.parseInt)
  }
  override def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    addPreferencesFromResource(R.xml.settings)
    contactList = findPreference(Constants.KEY_DELETE_CONTACT_GROUP).asInstanceOf[MultiSelectListPreference]
    findPreference("cleanse").setOnPreferenceClickListener(_ => {
      requestPermissions(false, Constants.PERMISSION_REQUEST_SMS, Array(PermissionFragment.WRITE_SMS), results => {
        val activity = getActivity
        if (results.forall(r => r)) {
          val dialog = ProgressDialog.show(activity, "", "Initializing...", true)
          Future {
            try {
              runOnUiThread(dialog.setTitle("Retrieving contacts..."))
              val groups = getContactGroups
              val strangers = groups.remove(Constants.STRANGERS)
              val contacts = new mutable.HashSet[Long]
              val resolver = activity.getContentResolver
              CursorHelper.forEach(resolver.query(ContactsContract.Data.CONTENT_URI, Array("raw_contact_id"),
                "(%s) AND mimetype = '%s'".formatLocal(Locale.ENGLISH, if (groups.isEmpty) "0"
                  else groups.map(i => GroupMembership.GROUP_ROW_ID + " = " + i).mkString(" OR "),
                  GroupMembership.CONTENT_ITEM_TYPE), null, null))(cursor =>
                contacts.add(cursor.getLong(0)))
              runOnUiThread(dialog.setTitle("Deleting SMS..."))
              val queryBuilder = new StringBuilder(Telephony.TextBasedSmsColumns.TYPE)
              queryBuilder.append(" = ")
              queryBuilder.append(Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT)
              queryBuilder.append(" AND (")
              if (strangers) {
                queryBuilder.append(Telephony.TextBasedSmsColumns.PERSON)
                queryBuilder.append(" IS NULL")
              }
              if (contacts.nonEmpty) {
                if (strangers) queryBuilder.append(" OR ")
                queryBuilder.append(Telephony.TextBasedSmsColumns.PERSON)
                queryBuilder.append(" IN (")
                queryBuilder.append(contacts.mkString(", "))
                queryBuilder.append(')')
              }
              queryBuilder.append(')')
              runOnUiThread(makeToast("%d SMS deleted. You may need to clear your default SMS app data afterwards."
                .formatLocal(Locale.ENGLISH,
                  resolver.delete(Telephony.Sms.CONTENT_URI, queryBuilder.toString, null))).show())
              //resolver.query(Telephony.Sms.CONTENT_URI, null, queryBuilder.toString, null, null).getCount)).show())
            } catch {
              case e: Exception =>
                e.printStackTrace()
                runOnUiThread(makeToast(e.getMessage).show())
            } finally runOnUiThread {
              dialog.cancel()
              releaseWriteSmsPermission()
            }
          }
        } else {
          makeToast("Not enough SMS permission granted, cannot proceed.").show()
          releaseWriteSmsPermission()
        }
      })
      true
    })
  }

  override def onDisplayPreferenceDialog(preference: Preference) = preference.getKey match {
    case Constants.KEY_DELETE_CONTACT_GROUP =>
      requestPermissions(false, Constants.PERMISSION_REQUEST_CONTACT,
        Array(android.Manifest.permission.READ_CONTACTS), results => {
        val ids = new ArrayBuffer[CharSequence]()
        val titles = new ArrayBuffer[CharSequence]()
        ids.append(Constants.STRANGERS.toString)
        titles.append("Strangers")
        if (results.forall(r => r))
          CursorHelper.forEach(getActivity.getContentResolver.query(ContactsContract.Groups.CONTENT_URI,
            Array(BaseColumns._ID, "title"), null, null, "title ASC")) { cursor =>
            ids.append(cursor.getInt(0).toString)
            titles.append(cursor.getString(1))
          }
        contactList.setEntries(titles.toArray)
        contactList.setEntryValues(ids.toArray)
        super.onDisplayPreferenceDialog(contactList)
      })
    case _ => super.onDisplayPreferenceDialog(preference)
  }
}
