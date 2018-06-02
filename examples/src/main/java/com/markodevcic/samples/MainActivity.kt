package com.markodevcic.samples

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.markodevcic.peko.*
import com.markodevcic.peko.rationale.AlertDialogPermissionRationale
import com.markodevcic.peko.rationale.SnackBarRationale
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {

	private var job = Job()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)

		if (isRequestingPermissions()) {
			launch (UI) {
				val result = permissionsResultDeferred!!.await()
				setResults(result)
			}
		}

		btnFineLocation.setOnClickListener {
			clearResults()
			requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		}
		btnFile.setOnClickListener {
			clearResults()
			requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		}
		btnCamera.setOnClickListener {
			clearResults()
			requestPermission(Manifest.permission.CAMERA)
		}
		btnAll.setOnClickListener {
			clearResults()
			requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
		}
		btnAllSnackBarRationale.setOnClickListener {
			clearResults()
			requestPermissionWithSnackBarRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
		}
	}

	private fun requestPermission(vararg permissions: String) {
		launch(job + UI) {
			val rationale = AlertDialogPermissionRationale(this@MainActivity) {
				this.setTitle("Need permissions")
				this.setMessage("Please give permissions to use this feature")
			}
			val result = requestPermissionsAsync(*permissions, rationale = rationale).await()
			setResults(result)
		}
	}

	private fun requestPermissionWithSnackBarRationale(vararg permissions: String) {
		val snackBar = Snackbar.make(rootView, "Permissions needed to continue", Snackbar.LENGTH_LONG)
		val snackBarRationale = SnackBarRationale(snackBar, "Request again")
		launch(job + UI) {
			val result = requestPermissionsAsync(*permissions, rationale = snackBarRationale).await()
			setResults(result)
		}
	}

	private fun setResults(result: PermissionRequestResult) {
		val (grantedPermissions, deniedPermissions) = result

		if (Manifest.permission.ACCESS_FINE_LOCATION in grantedPermissions) {
			textLocationResult.text = "GRANTED"
			textLocationResult.setTextColor(Color.GREEN)
		}
		if (Manifest.permission.WRITE_EXTERNAL_STORAGE in grantedPermissions) {
			textFileResult.text = "GRANTED"
			textFileResult.setTextColor(Color.GREEN)
		}
		if (Manifest.permission.CAMERA in grantedPermissions) {
			textCameraResult.text = "GRANTED"
			textCameraResult.setTextColor(Color.GREEN)
		}

		if (Manifest.permission.ACCESS_FINE_LOCATION in deniedPermissions) {
			textLocationResult.text = "DENIED"
			textLocationResult.setTextColor(Color.RED)
		}
		if (Manifest.permission.WRITE_EXTERNAL_STORAGE in deniedPermissions) {
			textFileResult.text = "DENIED"
			textFileResult.setTextColor(Color.RED)
		}
		if (Manifest.permission.CAMERA in deniedPermissions) {
			textCameraResult.text = "DENIED"
			textCameraResult.setTextColor(Color.RED)
		}
	}

	private fun clearResults() {
		textCameraResult.text = ""
		textFileResult.text = ""
		textLocationResult.text = ""
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_settings -> true
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isChangingConfigurations) {
			job.cancel(ActivityRotatingException())
		} else {
			job.cancel()
		}
	}
}
