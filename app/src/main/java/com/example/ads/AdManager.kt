package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {

    private const val TAG = "AdManager"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val reqConfig = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .build()
            MobileAds.setRequestConfiguration(reqConfig)

            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "Google Mobile Ads initialized: $initializationStatus")
                isInitialized = true
                // Do not eagerly load heavy rewarded video on cold launch to avoid
                // starting WebView video rendering processes and CORS unsafe header warnings.
                // Ads are preloaded just-in-time on demand.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    // ==========================================
    // INTERSTITIAL AD (Recommended trigger: After stopping recording or trimming clip)
    // ==========================================

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getInterstitialAdId()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isInterstitialLoading = false
                    _isInterstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                    _isInterstitialReady.value = false
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    preloadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    preloadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial shown")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready yet, skipping")
            preloadInterstitial(activity)
            onDismissed()
        }
    }

    // ==========================================
    // REWARDED AD (Recommended trigger: Unlock 2K/120FPS or Cloud Vault free)
    // ==========================================

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getRewardedAdId()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isRewardedLoading = false
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedReady.value = false
                    if (adUnitId != AdConfig.TEST_REWARDED_AD_ID) {
                        Log.d(TAG, "Retrying with Google Test Rewarded Ad Unit fallback...")
                        val testRequest = AdRequest.Builder().build()
                        RewardedAd.load(
                            context,
                            AdConfig.TEST_REWARDED_AD_ID,
                            testRequest,
                            object : RewardedAdLoadCallback() {
                                override fun onAdLoaded(testAd: RewardedAd) {
                                    rewardedAd = testAd
                                    _isRewardedReady.value = true
                                }
                                override fun onAdFailedToLoad(e: LoadAdError) {
                                    Log.w(TAG, "Fallback test rewarded ad also failed: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onDismissed: () -> Unit = {},
        onAdUnavailable: (() -> Unit)? = null
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded dismissed")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    preloadRewarded(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Rewarded failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    preloadRewarded(activity)
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded shown")
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.type} (${rewardItem.amount})")
                onRewardEarned(rewardItem)
            }
        } else {
            Log.d(TAG, "Rewarded ad not ready yet")
            preloadRewarded(activity)
            if (onAdUnavailable != null) {
                onAdUnavailable()
            } else {
                onDismissed()
            }
        }
    }

    // ==========================================
    // NATIVE ADVANCED AD (Recommended trigger: Gallery Grid card)
    // ==========================================

    fun loadNativeAd(
        context: Context,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: (LoadAdError) -> Unit = {}
    ) {
        val adUnitId = AdConfig.getNativeAdId()
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val nativeAdOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
            .build()

        val adLoader = AdLoader.Builder(context, adUnitId)
            .withNativeAdOptions(nativeAdOptions)
            .forNativeAd { nativeAd ->
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                    onAdFailed(error)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}
