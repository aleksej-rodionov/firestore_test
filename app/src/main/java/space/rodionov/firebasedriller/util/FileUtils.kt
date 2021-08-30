package space.rodionov.firebasedriller.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.config.Configurations
import java.io.File

//================================EXPORT=================================

fun generateFile(context: Context, fileName: String): File? {
    val csvFile = File(context.filesDir, fileName)
    csvFile.createNewFile()

    return if (csvFile.exists()) {
        csvFile
    } else {
        null
    }
}

// opens file
fun goToFileIntent(context: Context, file: File) : Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mimeType = context.contentResolver.getType(contentUri)
    intent.setDataAndType(contentUri, mimeType)
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    return intent
}

//===============================IMPORT=====================================

fun filePickerIntent(context: Context) : Intent {
    val intent = Intent(context, FilePickerActivity::class.java)
    intent.putExtra(FilePickerActivity.CONFIGS, Configurations.Builder()
        .setCheckPermission(true)
        .setShowFiles(true)
        .setShowImages(false)
        .setShowVideos(false)
        .setMaxSelection(1)
        .setSuffixes("csv", "txt")
        .setSkipZeroSizeFiles(true)
        .build())

    return intent
}





