package com.markodevcic.peko

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import com.markodevcic.peko.rationale.PermissionRationale
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import java.util.concurrent.atomic.AtomicReference


private val serviceReference = AtomicReference<PekoService?>(null)

/**
 * Get's the [Deferred] of [PermissionRequestResult] that is in progress
 * Returns null if there is no request in progress
 */
val Activity.permissionsResultDeferred: CompletableDeferred<PermissionRequestResult>?
  get() = serviceReference.get()?.deferredResult

/**
 * Requests [permissions] asynchronously.
 * This class is thread safe.
 * @return [Deferred] instance. Call [Deferred.await] inside a coroutine to get a [PermissionRequestResult]
 * @throws [IllegalStateException] if called while another request has not completed yet
 */
fun Activity.requestPermissionsAsync(vararg permissions: String,
                                     rationale: PermissionRationale = PermissionRationale.EMPTY): Deferred<PermissionRequestResult> {

  val request = checkPermissions(this, permissions)
  if (isTargetSdkUnderAndroidM(this)) {
    return CompletableDeferred(PermissionRequestResult(listOf(), permissions.toList()))
  }

  return if (request.denied.isNotEmpty()) {
    val service = PekoService(this, request, rationale,
        this.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE))

    if (!serviceReference.compareAndSet(null, service)) {
      throw IllegalStateException("Can't request permission while another request in progress")
    }

    service.requestPermissions().apply {
      invokeOnCompletion {
        if (it?.javaClass != ActivityRotatingException::class.java) {
          serviceReference.set(null)
        }
      }
    }
  } else {
    CompletableDeferred(PermissionRequestResult(request.granted, request.denied))
  }
}

private fun isTargetSdkUnderAndroidM(context: Context): Boolean {
  return try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val targetSdkVersion = info.applicationInfo.targetSdkVersion
    targetSdkVersion < Build.VERSION_CODES.M
  } catch (fail: PackageManager.NameNotFoundException) {
    false
  }
}

private fun checkPermissions(context: Context, permissions: Array<out String>): PermissionRequest {
  val permissionsGroup = permissions.groupBy { p -> ActivityCompat.checkSelfPermission(context, p) }
  val denied = permissionsGroup[PackageManager.PERMISSION_DENIED] ?: listOf()
  val granted = permissionsGroup[PackageManager.PERMISSION_GRANTED] ?: listOf()
  return PermissionRequest(granted, denied)
}

/**
 * Checks if there is a request in progress.
 * If true is returned, get the [Deferred] of [PermissionRequestResult] that is in progress by accessing [permissionsResultDeferred] property
 */
fun Activity.isRequestingPermissions(): Boolean = serviceReference.get() != null