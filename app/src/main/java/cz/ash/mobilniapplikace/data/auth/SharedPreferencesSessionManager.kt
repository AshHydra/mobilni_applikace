package cz.ash.mobilniapplikace.data.auth

import android.content.Context
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.Json

/**
 * Persists Supabase session to SharedPreferences so login survives app restarts.
 *
 * Note: for higher security you can switch to EncryptedSharedPreferences later.
 */
class SharedPreferencesSessionManager(
    context: Context,
    private val prefsName: String = "supabase_auth",
    private val key: String = "user_session",
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SessionManager {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val encoded = json.encodeToString(UserSession.serializer(), session)
        prefs.edit().putString(key, encoded).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(key).apply()
    }
}

