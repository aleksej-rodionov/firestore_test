package space.rodionov.firebasedriller.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import space.rodionov.firebasedriller.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao() : NoteDao

    class Callback @Inject constructor(
        private val database: Provider<NoteDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope // CoroutineScope from Dagger, but we added @ApplicationScope to tell  dagger "it's not just any coroutineScope here, it's the ApplicationScope"
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val dao = database.get().noteDao()

            // here can add initial data
        }
    }

}





