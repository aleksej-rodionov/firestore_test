package space.rodionov.firebasedriller.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat

@Entity(tableName = "note_table")
@Parcelize
data class Note(
    val text: String = "",
    val important: Boolean = false,
    val completed: Boolean = false,
    val created: Long = System.currentTimeMillis(),
    val authorId: String = "",
    @PrimaryKey(autoGenerate = true) val roomId: Int = 0
) : Parcelable {
    val createdDateFormatted: String
        get() = SimpleDateFormat.getTimeInstance().format(created)
}