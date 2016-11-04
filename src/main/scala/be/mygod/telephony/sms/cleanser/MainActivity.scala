package be.mygod.telephony.sms.cleanser

import android.os.Bundle
import be.mygod.app.ToolbarActivity

class MainActivity extends ToolbarActivity {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    configureToolbar()
  }
}
