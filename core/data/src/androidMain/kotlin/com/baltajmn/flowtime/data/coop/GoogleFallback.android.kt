package com.baltajmn.flowtime.data.coop

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google

actual suspend fun CoopAccount.signInWithGoogleFallback() {
    client.auth.signInWith(Google)
}
