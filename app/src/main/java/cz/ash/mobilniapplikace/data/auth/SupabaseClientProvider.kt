package cz.ash.mobilniapplikace.data.auth

import android.content.Context
import cz.ash.mobilniapplikace.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth

object SupabaseClientProvider {
    @Volatile private var INSTANCE: SupabaseClient? = null

    fun get(context: Context): SupabaseClient {
        val appContext = context.applicationContext
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth) {
                    // Persist session so login survives app restarts.
                    sessionManager = SharedPreferencesSessionManager(appContext)
                }
                install(Postgrest)
            }.also { INSTANCE = it }
        }
    }
}

