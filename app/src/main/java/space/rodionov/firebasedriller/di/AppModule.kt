package space.rodionov.firebasedriller.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import space.rodionov.firebasedriller.data.NoteDatabase
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        callback: NoteDatabase.Callback
    ) = Room.databaseBuilder(app, NoteDatabase::class.java, "note_database")
        .fallbackToDestructiveMigration()
        .addCallback(callback)
        .build()

    @Provides
    @Singleton
    fun provideNoteDao(db: NoteDatabase) = db.noteDao()

    @ApplicationScope // we tell here dagger "it's not just a coroutineScope, it's the ApplicationScope"
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob()) // here we created our own coroutine scope
    // that lives as long as our application lives. We can use it for long-running operations of our whole app
    // but by default a coroutine gets cancelled when any of its child fails.
    // And if there are 2 running coroutines in it and one of them fails, scope casts the second one to begins of its way.
    // To avoid this, we added SupervisorJob(), which tells toe coroutine to keep the child running when the other child fails.
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope // create our own annotation
//  with which we can tell dagger "it's not just a coroutineScope, it's the ApplicationScope"
// so that if we later have 2 different coroutine Scopes in dagger, there'll not be any ambuguity
// cause dagger will know that "Well, the one coroutine scope we expect here is the @ApplicationScope".



