package com.sarsonasgino.quickinvoicemobile.core.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "quickinvoice_prefs"
    private const val KEY_TOKEN = "clerk_token"
    private const val KEY_CLERK_ID = "clerk_id"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHOTO_URL = "photo_url"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveClerkId(context: Context, clerkId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLERK_ID, clerkId).apply()
    }

    fun getClerkId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLERK_ID, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }


    fun saveUserInfo(context: Context, firstName: String, lastName: String, email: String, photoUrl: String = "") {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHOTO_URL, photoUrl)
            .apply()
    }

    fun getFirstName(context: Context): String = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_FIRST_NAME, "") ?: ""
    fun getLastName(context: Context): String = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_LAST_NAME, "") ?: ""
    fun getEmail(context: Context): String = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_EMAIL, "") ?: ""
    fun getPhotoUrl(context: Context): String = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_PHOTO_URL, "") ?: ""
}