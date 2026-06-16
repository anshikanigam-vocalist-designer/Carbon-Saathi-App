package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Database Entities ---

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val ageGroup: String, // "Teens (Ravi)", "Parents (Meera)", "Seniors (Dadu)", "Farmers (Sanjay)", "Kids (Aarav)", "Students (Priya)"
    val location: String, // City, Village, Small Town
    val preferredLanguage: String, // "Hindi", "English", "Hinglish"
    val userRole: String, // "Student", "Corporate", "Farmer", "Senior", "HomeMaker"
    val totalCo2Saved: Double = 0.0,
    val dailyStreak: Int = 0,
    val lastActionTimestamp: Long = 0,
    val currentPoints: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user", "saathi"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false
)

@Entity(tableName = "tracking_logs")
data class TrackingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Transport", "Energy", "Food", "Farming", "Shopping"
    val activityName: String, // "Metro Ride", "Bulb switched off", "Mulching instead of crop burning", etc.
    val quantity: Double,
    val co2Saved: Double, // in kg of CO2
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenge_logs")
data class ChallengeLog(
    @PrimaryKey val challengeId: String,
    val title: String,
    val category: String,
    val co2Saved: Double,
    val completedTimestamp: Long = System.currentTimeMillis()
)

// --- DAOs (Data Access Objects) ---

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET totalCo2Saved = totalCo2Saved + :co2, currentPoints = currentPoints + :points WHERE id = 1")
    suspend fun incrementCo2AndPoints(co2: Double, points: Int)

    @Query("UPDATE user_profile SET dailyStreak = :streak, lastActionTimestamp = :timestamp WHERE id = 1")
    suspend fun updateStreak(streak: Int, timestamp: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface TrackingLogDao {
    @Query("SELECT * FROM tracking_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TrackingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TrackingLog)

    @Query("SELECT SUM(co2Saved) FROM tracking_logs")
    fun getTotalCo2Saved(): Flow<Double?>
}

@Dao
interface ChallengeLogDao {
    @Query("SELECT * FROM challenge_logs")
    fun getAllCompletedChallenges(): Flow<List<ChallengeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun completeChallenge(challenge: ChallengeLog)

    @Query("DELETE FROM challenge_logs")
    suspend fun clearChallenges()
}

// --- App Database Class ---

@Database(
    entities = [UserProfile::class, ChatMessage::class, TrackingLog::class, ChallengeLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun trackingLogDao(): TrackingLogDao
    abstract fun challengeLogDao(): ChallengeLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "carbon_saathi_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository Pattern Implementation ---

class CarbonRepository(private val db: AppDatabase) {
    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()
    val chatMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessages()
    val trackingLogs: Flow<List<TrackingLog>> = db.trackingLogDao().getAllLogs()
    val completedChallenges: Flow<List<ChallengeLog>> = db.challengeLogDao().getAllCompletedChallenges()

    suspend fun saveProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdateProfile(profile)
    }

    suspend fun recordCo2Saved(co2: Double, points: Int, category: String, activity: String, qty: Double) {
        // Increment profile values
        db.userProfileDao().incrementCo2AndPoints(co2, points)
        // Record log
        db.trackingLogDao().insertLog(
            TrackingLog(
                category = category,
                activityName = activity,
                quantity = qty,
                co2Saved = co2
            )
        )
    }

    suspend fun updateStreakIfNeeded(profile: UserProfile) {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val twoDaysMillis = 48 * 60 * 60 * 1000L

        val lastTime = profile.lastActionTimestamp
        val currentStreak = profile.dailyStreak

        if (lastTime == 0L) {
            db.userProfileDao().updateStreak(1, currentTime)
        } else {
            val diff = currentTime - lastTime
            when {
                diff in oneDayMillis until twoDaysMillis -> {
                    // Streaks continues!
                    db.userProfileDao().updateStreak(currentStreak + 1, currentTime)
                }
                diff >= twoDaysMillis -> {
                    // Reset streak
                    db.userProfileDao().updateStreak(1, currentTime)
                }
                diff < oneDayMillis -> {
                    // Do nothing, same day
                }
            }
        }
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        db.chatMessageDao().insertMessage(message)
    }

    suspend fun clearChatHistory() {
        db.chatMessageDao().clearChat()
    }

    suspend fun completeChallenge(challengeId: String, title: String, category: String, co2Saved: Double) {
        db.challengeLogDao().completeChallenge(
            ChallengeLog(
                challengeId = challengeId,
                title = title,
                category = category,
                co2Saved = co2Saved
            )
        )
        db.userProfileDao().incrementCo2AndPoints(co2Saved, 15)
    }

    suspend fun clearAllChallenges() {
        db.challengeLogDao().clearChallenges()
    }
}
