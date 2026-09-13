package com.example.ads

import com.example.BuildConfig

/**
 * AdMob configuration holding the ad unit IDs provided by the user,
 * along with Google's official test ad unit IDs for safe offline/debug verification.
 */
object AdConfig {
    // User's Real Production AdMob Ad Unit IDs
    const val LIVE_INTERSTITIAL_AD_ID = "ca-app-pub-2407608007371544/9223218003"
    const val LIVE_NATIVE_AD_ID = "ca-app-pub-2407608007371544/9849903964"
    const val LIVE_REWARDED_AD_ID = "ca-app-pub-2407608007371544/3434232521"
    const val LIVE_BANNER_AD_ID = "ca-app-pub-2407608007371544/8391045735"

    // Google Official Test Ad Unit IDs (guaranteed to render test creatives safely during development)
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247690489"
    const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"

    // In debug builds or development emulator, default to official Google test units
    // to prevent invalid traffic or ad format mismatches. Can also be toggled in Settings.
    var useTestAds: Boolean = BuildConfig.DEBUG

    fun getInterstitialAdId(): String = if (useTestAds) TEST_INTERSTITIAL_AD_ID else LIVE_INTERSTITIAL_AD_ID
    fun getNativeAdId(): String = if (useTestAds) TEST_NATIVE_AD_ID else LIVE_NATIVE_AD_ID
    fun getRewardedAdId(): String = if (useTestAds) TEST_REWARDED_AD_ID else LIVE_REWARDED_AD_ID
    fun getBannerAdId(): String = if (useTestAds) TEST_BANNER_AD_ID else LIVE_BANNER_AD_ID
}
