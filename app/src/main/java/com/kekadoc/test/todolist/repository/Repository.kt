package com.kekadoc.test.todolist.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import android.util.Base64
import androidx.paging.DataSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayOutputStream

private const val TAG: String = "Repo-TAG"

object Converter {
        @TypeConverter
        fun bitMapToString(bitmap: Bitmap?): String? {
            if (bitmap == null) return null
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }
        @TypeConverter
        fun stringToBitMap(encodedString: String?): Bitmap? {
            if (encodedString == null) return null
            return try {
                val encodeBytes = Base64.decode(encodedString, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(encodeBytes, 0, encodeBytes.size)
            } catch(e: Throwable) {
                return null
            }
        }
}

@Parcelize
@Entity(tableName = "Tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timeOfCreation") val timeOfCreation: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "timeOfCompletion") val timeOfCompletion: Long? = null,
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "image") val image: String? = null,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "complete") val isComplete: Boolean = false,
): Parcelable

val Task.exist: Boolean
    get() = id > 0

val Task.uniqueName: String
    get() = id.toString()

@Dao
interface TaskDao {

    @Query("SELECT * FROM Tasks")
    suspend fun getAll(): List<Task>
    @Query("SELECT * FROM Tasks WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Task>
    @Query("SELECT * FROM Tasks WHERE id = :id")
    suspend fun get(id: Long): Task?
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(task: Task)
    @Query("SELECT COUNT(*) FROM Tasks")
    suspend fun getCount(): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<Task>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: Task): Long
    @Delete
    suspend fun delete(user: Task)
    @Query("DELETE FROM Tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM Tasks WHERE id = :id")
    fun getFlow(id: Long): Flow<Task>
    @Query("SELECT * FROM Tasks")
    fun getAllFlow(): Flow<List<Task>>

    @Query("SELECT * FROM Tasks ORDER BY complete ASC, timeOfCompletion DESC, timeOfCreation DESC")
    fun getAllPaged(): DataSource.Factory<Int, Task>

}

@TypeConverters(Converter::class)
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasksDao(): TaskDao
}

class Repository private constructor(db: AppDatabase) {

    companion object {

        @Volatile private var instance: Repository? = null
        fun getInstance(context: Context, db: AppDatabase? = null): Repository {
            return instance ?: synchronized<Repository>(this) {
                if (instance != null) return@synchronized instance!!
                val noNullDb: AppDatabase = db ?: Room.databaseBuilder(
                    context, AppDatabase::class.java, AppDatabase::class.simpleName!!
                ).build()
                Repository(noNullDb).also { instance = it }
            }
        }

    }

    private val db = db
    val tasks = this.db.tasksDao()

    fun release() {
        db.close()
    }

}

