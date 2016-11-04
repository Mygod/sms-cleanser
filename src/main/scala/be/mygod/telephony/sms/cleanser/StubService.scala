package be.mygod.telephony.sms.cleanser

import android.app.Service
import android.content.Intent

class StubService extends Service {
  override final def onBind(intent: Intent) = null
}
