package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Default Google AdMob Test Banner ID
    useSimulatedAds: Boolean = false
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val amberAccent = Color(0xFFFFB300)
    val warmWood = Color(0xFF8D6E63)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF0F0F13) else Color(0xFFF9F7F1))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF23232C) else Color(0xFFE5E2D9)
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (useSimulatedAds) {
            SimulatedSponsorAd(isDark = isDark)
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = adUnitId
                        adListener = object : com.google.android.gms.ads.AdListener() {
                            override fun onAdLoaded() {
                                isAdLoaded = true
                                loadError = null
                            }

                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                isAdLoaded = false
                                loadError = error.message
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { adView ->
                    // AdView automatically manages state, but we can call load here if needed
                }
            )

            // Show a visually stunning, context-appropriate backup ad if AdMob takes a second or fails in local JVM test
            if (!isAdLoaded) {
                SimulatedSponsorAd(isDark = isDark, text = if (loadError != null) "Sustaining Project Themis • Supporting Ad" else "Loading Google Mobile Ads...")
            }
        }
    }
}

@Composable
private fun SimulatedSponsorAd(
    isDark: Boolean,
    text: String = "Sustain AI Credits • Powered by Google Ads"
) {
    val themeColor = if (isDark) Color(0xFFFFB300) else Color(0xFF8D6E63)
    val cardBg = if (isDark) Color(0xFF16161B) else Color(0xFFF0EBE1)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Ad Badge
                Surface(
                    color = themeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = Brush.linearGradient(listOf(themeColor, themeColor))),
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = "AD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Column {
                    Text(
                        text = "Project Themis Premium Credit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = text,
                        fontSize = 9.sp,
                        color = if (isDark) Color.Gray else Color.DarkGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // CTA / Icon button representing support
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Support Project",
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SUPPORT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
