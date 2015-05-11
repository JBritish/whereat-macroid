package org.tlc.whereat.activities

import android.app.Activity
import android.content.{ComponentName, Intent}
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.widget.{Button, LinearLayout, TextView}
import macroid.FullDsl._
import macroid.{Ui, AppContext, Contexts}
import org.tlc.whereat.model.Conversions
import org.tlc.whereat.msg.Logger
import org.tlc.whereat.services.{GoogleApiService, IntersectionService}
import org.tlc.whereat.ui.tweaks.MainTweaks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
 * Author: @aguestuser
 * Date: 4/22/15
 * License: GPLv2 (https://www.gnu.org/licenses/gpl-2.0.html)
 */


class MainActivity extends Activity
  with Contexts[Activity]
  with GoogleApiService
  with IntersectionService
  with Conversions
  with Logger {

  implicit lazy val appContextProvider: AppContext = activityAppContext
  var intersectionView: Option[TextView] = slot[TextView]
  var sharePrompt = slot[Button]

  // Activy UI & life cycle methods

  override protected def onCreate(savedInstanceState: Bundle): Unit = {
    super .onCreate(savedInstanceState)

    gApiClient = buildGoogleApiClient(this)

    setContentView {
      getUi {
        l[LinearLayout](
          w[Button] <~
            text("Get Location") <~
            On.click {
              Ui.sequence(
                intersectionView <~ getIntersection.map { i ⇒ text(i) + show },
                sharePrompt <~ show
              )
            },
          w[TextView] <~
            wire(intersectionView) <~ hide,
          w[Button] <~
            wire(sharePrompt) <~
            text("Share Location") <~ hide
        ) <~ MainTweaks.orient } } }

  override protected def onStart(): Unit = {
    super.onStart()
    gApiClient foreach { _.connect } }

  override protected def onStop(): Unit = {
    super.onStop()
    gApiClient foreach { cl ⇒ if(cl.isConnected) cl.disconnect() } }

  // location sharing

  def getIntersection: Future[String] =
    getLocation flatMap {
      case None ⇒ Future.successful("Location not available") //Future.successful ("Location not available")
      case Some(l) ⇒ geocodeLocation(toLoc(l)) map parseGeocoding }

  def shareIntersection(intersection: String): Future[Unit] =
    getPhoneNumber flatMap { num ⇒
      phoneNumberPromise = Promise()
      sendSms(intersection)(num) }

  // contact intent passing
  // TODO extract this to a trait!

  var phoneNumberPromise: Promise[String] = Promise()
  val REQUEST_SELECT_PHONE_NUMBER = 1
  val RESULT_OK = -1

  def getPhoneNumber: Future[String] = {
    val intent = new Intent(Intent.ACTION_PICK).setType(CommonDataKinds.Phone.CONTENT_TYPE)
    resolve(intent) foreach  { _ ⇒ startActivityForResult(intent, REQUEST_SELECT_PHONE_NUMBER) }
    phoneNumberPromise.future }

  protected override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    if (requestCode == REQUEST_SELECT_PHONE_NUMBER && resultCode == RESULT_OK) {
      val (uri, projection) = (data.getData, Array(CommonDataKinds.Phone.NUMBER))
      val cursor = getContentResolver.query(uri, projection, null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val numberIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER)
        phoneNumberPromise.success { cursor.getString(numberIndex) } } } }

  // sms intent passing
  // TODO implement this and extract it to a trait!

  def sendSms(msg: String)(recipient: String): Future[Unit] = {
    val intent = new Intent(Intent.ACTION_SEND)
      .setData(Uri.parse("smsto:"))
      .putExtra("sms_body", msg)
    resolve(intent) foreach { _ ⇒ startActivity(intent) }
    Future.successful{()}
  }


  def resolve(intent: Intent): Option[ComponentName] =
    Option(intent.resolveActivity(getPackageManager))
}

