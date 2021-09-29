package com.github.fruzelee.eventcalendar.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.helpers.MyContextWrapper
import java.util.*

/**
 * @author fazle
 * Created 9/25/2021 at 12:56 PM
 * github.com/fruzelee
 * web: fr.crevado.com
 */
abstract class BaseEventActivity : AppCompatActivity() {
    var copyMoveCallback: ((destinationPath: String) -> Unit)? = null
    private var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    private var isAskingPermissions = false
    var useDynamicTheme = true
    private var showTransparentTop = false
    var checkedDocumentPath = ""
    var configItemsToExport = LinkedHashMap<String, Any>()

    companion object {
        var funAfterSAFPermission: ((success: Boolean) -> Unit)? = null
        private const val GENERIC_PERM_HANDLER = 100
    }

    abstract fun getAppIconIDs(): ArrayList<Int>

    abstract fun getAppLauncherName(): String

    override fun onResume() {
        super.onResume()
        if (showTransparentTop) {
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    override fun onStop() {
        super.onStop()
        actionOnPermission = null
    }

    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun attachBaseContext(newBase: Context) {
        if (newBase.baseConfig.useEnglish) {
            super.attachBaseContext(MyContextWrapper(newBase).wrap(newBase, "en"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    fun updateBackgroundColor(color: Int = baseConfig.backgroundColor) {
        window.decorView.setBackgroundColor(color)
    }

    @Suppress("DEPRECATION")
    fun setTranslucentNavigation() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
    }


    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()

        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
    }

    private fun isProperSDFolder(uri: Uri) =
        isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isProperOTGFolder(uri: Uri) =
        isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")

    private fun isInternalStorage(uri: Uri) =
        isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri)
            .contains("primary")

    private fun isExternalStorageDocument(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority


    @RequiresApi(Build.VERSION_CODES.O)
    fun launchCustomizeNotificationsIntent() {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(this)
        }
    }


    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(
                this,
                arrayOf(getPermissionString(permissionId)),
                GENERIC_PERM_HANDLER
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isAskingPermissions = false
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }


    private fun getExportSettingsFilename(): String {
        var defaultFilename = baseConfig.lastExportedSettingsFile
        if (defaultFilename.isEmpty()) {
            val appName = baseConfig.appId.removeSuffix(".debug").removeSuffix(".pro")
                .removePrefix("com.github.fruzelee.")
            defaultFilename = "$appName-settings.txt"
        }

        return defaultFilename
    }

    fun updateMenuItemColors(
        menu: Menu?,
        useCrossAsBack: Boolean = false,
        baseColor: Int = baseConfig.primaryColor
    ) {
        if (menu == null) {
            return
        }

        val color = baseColor.getContrastColor()
        for (i in 0 until menu.size()) {
            try {
                menu.getItem(i)?.icon?.setTint(color)
            } catch (ignored: Exception) {
            }
        }

        val drawableId =
            if (useCrossAsBack) R.drawable.ic_cross_vector else R.drawable.ic_arrow_left_vector
        val icon = resources.getColoredDrawableWithColor(drawableId, color)
        supportActionBar?.setHomeAsUpIndicator(icon)
    }
}
