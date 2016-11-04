package be.mygod.telephony.sms.cleanser

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.support.v13.app.FragmentCompat
import android.support.v4.content.ContextCompat
import be.mygod.app.FragmentPlus

object PermissionFragment {
  final val WRITE_SMS = "android.permission.WRITE_SMS"

  private var oldSmsPackage: String = _
}

trait PermissionFragment extends FragmentPlus with FragmentCompat.OnRequestPermissionsResultCallback {
  import PermissionFragment._

  private case class PermissionRequest(permissions: Array[String], callback: Array[Boolean] => Unit)
  private val requests = new scala.collection.mutable.HashMap[Int, PermissionRequest]()

  def requestPermissions(silent: Boolean, requestCode: Int, permissions: Array[String],
                         callback: Array[Boolean] => Unit) = {
    val activity = getActivity
    val self = activity.getPackageName
    val results = permissions.map(permission => permission != WRITE_SMS &&
      ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED ||
      (Telephony.Sms.getDefaultSmsPackage(activity) match {
        case `self` => true
        case p =>
          oldSmsPackage = p
          false
      }))
    if (silent || results.forall(r => r)) callback(results) else {
      requests(requestCode) = PermissionRequest(permissions, callback)
      if (oldSmsPackage != null) startActivityForResult(new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, self), requestCode)
      else FragmentCompat.requestPermissions(this, permissions, requestCode)
    }
  }

  def releaseWriteSmsPermission() = if (oldSmsPackage != null) {
    val activity = getActivity
    if (activity.getPackageName == Telephony.Sms.getDefaultSmsPackage(activity))
      startActivity(new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, oldSmsPackage))
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = requests.get(requestCode) match {
    case Some(request) => if (request.permissions.exists(p => p != WRITE_SMS))
      FragmentCompat.requestPermissions(this, request.permissions, requestCode) else {
      val result =
        if (resultCode == Activity.RESULT_OK) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
      onRequestPermissionsResult(requestCode, request.permissions, request.permissions.map(p => result))
    }
    case None => super.onActivityResult(requestCode, resultCode, data)
  }

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]) {
    val request = requests(requestCode)
    val n = permissions.length
    val results = new Array[Boolean](n)
    val activity = getActivity
    for (i <- 0 until n) results(i) = grantResults(i) == PackageManager.PERMISSION_GRANTED &&
      (permissions(i) != WRITE_SMS || activity.getPackageName == Telephony.Sms.getDefaultSmsPackage(activity))
    request.callback(results)
  }
}
