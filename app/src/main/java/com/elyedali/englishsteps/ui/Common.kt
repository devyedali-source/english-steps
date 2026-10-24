package com.elyedali.englishsteps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker

object Palette {
    val Primary = Color(0xFF4C3ED6)
    val PrimaryLight = Color(0xFFE9E6FF)
    val Yellow = Color(0xFFFFC93C)
    val Orange = Color(0xFFFF8C42)
    val Green = Color(0xFF22B45E)
    val GreenLight = Color(0xFFDDF7E7)
    val Red = Color(0xFFE5484D)
    val RedLight = Color(0xFFFFE3E3)
    val Background = Color(0xFFF6F5FF)
    val Ink = Color(0xFF1E1B3A)
    val Muted = Color(0xFF6E6A8A)
    val Locked = Color(0xFFD9D7E6)
    val levelColors = listOf(
        Color(0xFF22B45E), Color(0xFF14A3C7), Color(0xFF4C3ED6), Color(0xFFD9468F), Color(0xFFE5732C),
    )
}

val AppColors = lightColorScheme(
    primary = Palette.Primary,
    onPrimary = Color.White,
    primaryContainer = Palette.PrimaryLight,
    secondary = Palette.Orange,
    background = Palette.Background,
    surface = Color.White,
    onSurface = Palette.Ink,
    onBackground = Palette.Ink,
)

/** المحتوى الإنجليزي يبقى من اليسار إلى اليمين حتى في الواجهة العربية */
@Composable
fun Ltr(content: @Composable () -> kotlin.Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }
}

@Composable
fun Pic(emoji: String, size: TextUnit = 64.sp, modifier: Modifier = Modifier) {
    Text(emoji, fontSize = size, modifier = modifier, textAlign = TextAlign.Center)
}

@Composable
fun SpeakButton(text: String, modifier: Modifier = Modifier, big: Boolean = false) {
    val s = if (big) 64.dp else 40.dp
    Box(
        modifier
            .size(s)
            .clip(CircleShape)
            .background(Palette.Primary)
            .clickable { Speaker.speak(text) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(s * 0.55f))
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> kotlin.Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Primary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmoothProgress(fraction: Float, modifier: Modifier = Modifier, color: Color = Palette.Green, height: Dp = 14.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Palette.Locked),
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun StatPill(emoji: String, text: String, color: Color = Palette.Ink, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(1.dp, Palette.Locked, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text(text, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
    }
}

@Composable
fun GoalRing(fraction: Float, size: Dp = 46.dp, center: @Composable () -> kotlin.Unit) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.12f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Palette.Locked, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(Palette.Orange, -90f, 360f * f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
fun StarsRow(n: Int, size: TextUnit = 16.sp) {
    Row {
        repeat(3) { i -> Text(if (i < n) "⭐" else "☆", fontSize = size, color = Palette.Muted) }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> kotlin.Unit, close: Boolean = false, actions: @Composable () -> kotlin.Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(if (close) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    onClick: (() -> kotlin.Unit)? = null,
    content: @Composable () -> kotlin.Unit,
) {
    val m = if (onClick != null) modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() } else modifier
    Card(
        modifier = m,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) { content() }
}

@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable () -> kotlin.Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = modifier, color = Palette.Ink)
}

@Composable
fun Sub(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 16.sp, color = Palette.Muted, textAlign = TextAlign.Center, modifier = modifier)
}

@Composable
fun ThemeWrap(content: @Composable () -> kotlin.Unit) {
    MaterialTheme(colorScheme = AppColors) { content() }
}
