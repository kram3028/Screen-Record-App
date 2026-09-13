package com.example.data.config

/**
 * Centralized configuration for Cloud Storage features.
 *
 * HOW TO UNLOCK:
 * When you resolve the pricing/prize issue and want to remove the lock,
 * simply change [IS_CLOUD_STORAGE_LOCKED] from `true` to `false`.
 */
object CloudConfig {
    /**
     * Master lock switch for Cloud Storage.
     * Set to `true` to temporarily lock Cloud Storage for every user due to pricing issues.
     * Set to `false` to remove the lock and restore full access for all users.
     */
    const val IS_CLOUD_STORAGE_LOCKED: Boolean = true

    /**
     * User-facing title explaining the lock status.
     */
    const val LOCK_TITLE: String = "Cloud Storage Temporarily Locked"

    /**
     * User-facing detailed message explaining why Cloud Storage is paused.
     */
    const val LOCK_MESSAGE: String =
        "Cloud Storage and online vault features are temporarily locked for all users while we resolve a pricing and prize fixing issue. Backups and purchases will be re-enabled as soon as the update is finalized. Thank you for your patience!"

    /**
     * Short feedback message shown in snackbars when a user attempts a locked cloud action.
     */
    const val LOCK_SNACKBAR_MESSAGE: String =
        "Cloud Storage is temporarily locked due to pricing maintenance. Please check back soon."
}
