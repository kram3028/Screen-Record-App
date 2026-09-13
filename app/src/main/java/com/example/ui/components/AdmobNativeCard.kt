package com.example.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button as AndroidButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdManager
import com.example.ui.theme.RecorderRed
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun AdmobNativeCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AdManager.loadNativeAd(
            context = context,
            onAdLoaded = { ad ->
                nativeAd = ad
            },
            onAdFailed = {
                isFailed = true
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_native_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (nativeAd != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                factory = { ctx ->
                    createNativeAdView(ctx, nativeAd!!)
                },
                update = { nativeAdView ->
                    nativeAd?.let { populateNativeAdView(it, nativeAdView) }
                }
            )
        } else {
            // Organic placeholder card while loading or fallback
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RecorderRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            color = RecorderRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sponsored Content",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isFailed) "Sponsored space available" else "Loading partner...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun createNativeAdView(context: Context, ad: NativeAd): NativeAdView {
    val density = context.resources.displayMetrics.density
    val nativeAdView = NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Header: Ad Badge & Headline & Icon
    val headerLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val adBadge = TextView(context).apply {
        text = "AD"
        textSize = 10f
        setTypeface(null, Typeface.BOLD)
        setTextColor(AndroidColor.WHITE)
        setBackgroundColor(0xFFE50914.toInt())
        val padH = (6 * density).toInt()
        val padV = (2 * density).toInt()
        setPadding(padH, padV, padH, padV)
    }
    headerLayout.addView(adBadge)

    val iconSizePx = (36 * density).toInt()
    val iconMarginPx = (8 * density).toInt()
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            setMargins(iconMarginPx, 0, iconMarginPx, 0)
        }
    }
    headerLayout.addView(iconView)
    nativeAdView.iconView = iconView

    val headlineView = TextView(context).apply {
        textSize = 14f
        setTypeface(null, Typeface.BOLD)
        setTextColor(AndroidColor.WHITE)
        maxLines = 1
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    }
    headerLayout.addView(headlineView)
    nativeAdView.headlineView = headlineView

    rootLayout.addView(headerLayout)

    // Body Text
    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(0xFFAAAAAA.toInt())
        maxLines = 2
        val padV = (6 * density).toInt()
        setPadding(0, padV, 0, padV)
    }
    rootLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView

    // Media View (AdMob validator requires at least 120x120dp for video native ads)
    val minSizePx = (140 * density).toInt()
    val mediaHeightPx = (180 * density).toInt()
    val mediaView = MediaView(context).apply {
        minimumWidth = minSizePx
        minimumHeight = minSizePx
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            mediaHeightPx
        ).apply {
            setMargins(0, (4 * density).toInt(), 0, (6 * density).toInt())
        }
    }
    rootLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // CTA Button
    val ctaButton = AndroidButton(context).apply {
        setBackgroundColor(0xFFE50914.toInt())
        setTextColor(AndroidColor.WHITE)
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (44 * density).toInt()
        ).apply {
            setMargins(0, (6 * density).toInt(), 0, 0)
        }
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    populateNativeAdView(ad, nativeAdView)

    return nativeAdView
}

private fun populateNativeAdView(ad: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = ad.headline
    (nativeAdView.bodyView as? TextView)?.text = ad.body ?: ""

    val icon = ad.icon
    val iconView = nativeAdView.iconView as? ImageView
    if (icon != null && iconView != null) {
        iconView.setImageDrawable(icon.drawable)
        iconView.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val ctaButton = nativeAdView.callToActionView as? AndroidButton
    if (ad.callToAction != null && ctaButton != null) {
        ctaButton.text = ad.callToAction
        ctaButton.visibility = View.VISIBLE
    } else {
        ctaButton?.visibility = View.GONE
    }

    val mediaView = nativeAdView.mediaView
    if (mediaView != null && ad.mediaContent != null) {
        mediaView.mediaContent = ad.mediaContent
        mediaView.visibility = View.VISIBLE
    } else {
        mediaView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(ad)
}
