package be.mygod.telephony.sms.cleanser

import android.content.{BroadcastReceiver, Context, Intent}

class StubReceiver extends BroadcastReceiver {
  override final def onReceive(context: Context, intent: Intent) = ()
}

final class StubMmsReceiver extends StubReceiver { }
final class StubSmsReceiver extends StubReceiver { }
