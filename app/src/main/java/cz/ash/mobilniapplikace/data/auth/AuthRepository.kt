package cz.ash.mobilniapplikace.data.auth

import cz.ash.mobilniapplikace.domain.UserAccount
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

class AuthRepository(
    private val supabase: SupabaseClient
) {
    suspend fun loadFromStorage(): Boolean {
        // loads session via configured SessionManager (SharedPreferencesSessionManager)
        return supabase.auth.loadFromStorage(true)
    }

    fun currentUserOrNull(): UserAccount? {
        val user = supabase.auth.currentSessionOrNull()?.user ?: return null
        return UserAccount(
            id = user.id,
            email = user.email
        )
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}

