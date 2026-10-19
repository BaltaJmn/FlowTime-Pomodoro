package com.baltajmn.flowtime.data.coop

/**
 * Google cuando no hay inicio de sesion nativo (compose-auth lo llama como `fallback`). En Android, el
 * flujo web de Supabase; en el iPhone, el propio con el cliente iOS de FlowTime (GoogleSignInIos.kt).
 */
expect suspend fun CoopAccount.signInWithGoogleFallback()
