package be.mygod.telephony.sms.cleanser

import android.database.Cursor

object CursorHelper {
  def forEach(cursor: Cursor)(work: Cursor => Unit) = while (cursor != null && cursor.moveToNext) work(cursor)
}
