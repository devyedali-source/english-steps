package com.elyedali.englishsteps.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.LessonKind
import com.elyedali.englishsteps.data.LessonUnit
import com.elyedali.englishsteps.i18n.I18n
import com.elyedali.englishsteps.i18n.t

fun kindTitle(kind: LessonKind): String = when (kind) {
    LessonKind.VOCAB -> t("الكلمات", "Vocabulaire", "Words")
    LessonKind.GRAMMAR -> t("القواعد", "Grammaire", "Grammar")
    LessonKind.DIALOGUE -> t("المحادثة", "Conversation", "Conversation")
    LessonKind.PRACTICE -> t("تحدي الوحدة", "Défi de l'unité", "Unit challenge")
}

/** شاشة التعلّم التي تسبق التمارين */
@Composable
fun LessonIntro(unit: LessonUnit, kind: LessonKind, onBack: () -> kotlin.Unit, onStart: () -> kotlin.Unit) {
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }
    Column(Modifier.fillMaxSize().background(Palette.Background)) {
        TopBar("${unit.emoji} ${unit.title.get()} · ${kindTitle(kind)}", onBack = onBack)
        when (kind) {
            LessonKind.VOCAB -> VocabCards(unit, onStart)
            LessonKind.GRAMMAR -> GrammarPage(unit, onStart)
            LessonKind.DIALOGUE -> DialoguePage(unit, onStart)
            LessonKind.PRACTICE -> {}
        }
    }
}

@Composable
private fun VocabCards(unit: LessonUnit, onStart: () -> kotlin.Unit) {
    var i by remember { mutableIntStateOf(0) }
    val words = unit.vocab
    val w = words[i]
    LaunchedEffect(i) { Speaker.speak(w.en) }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SmoothProgress((i + 1).toFloat() / words.size, color = Palette.Primary, height = 10.dp)
        Spacer(Modifier.height(6.dp))
        Text("${i + 1} / ${words.size}", color = Palette.Muted)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = i,
            transitionSpec = { (slideInHorizontally { it / 2 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "card",
        ) { idx ->
            val word = words[idx]
            SoftCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Pic(word.pic, 120.sp)
                    Spacer(Modifier.height(12.dp))
                    Ltr {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(word.en, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Primary)
                            Spacer(Modifier.width(12.dp))
                            SpeakButton(word.en)
                        }
                    }
                    if (I18n.lang != "en") {
                        Spacer(Modifier.height(8.dp))
                        Text(word.meaning(), fontSize = 24.sp, color = Palette.Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = { Speaker.speak(word.en, slow = true) }) {
                        Text(t("🐢 استمع ببطء", "🐢 Écouter lentement", "🐢 Listen slowly"))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (i > 0) {
                OutlinedButton(onClick = { i -= 1 }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(t("السابق", "Précédent", "Back"), fontSize = 17.sp)
                }
            }
            if (i < words.size - 1) {
                BigButton(t("التالي", "Suivant", "Next"), onClick = { i += 1 }, modifier = Modifier.weight(1f))
            } else {
                BigButton(t("ابدأ التمارين", "Commencer les exercices", "Start exercises"), onClick = onStart, modifier = Modifier.weight(1f), color = Palette.Green)
            }
        }
    }
}

@Composable
private fun GrammarPage(unit: LessonUnit, onStart: () -> kotlin.Unit) {
    val g = unit.grammar
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Palette.PrimaryLight), contentAlignment = Alignment.Center) {
                    Text("🧩", fontSize = 28.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(g.title.get(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            }
            Spacer(Modifier.height(16.dp))
            SoftCard(Modifier.fillMaxWidth(), color = Color(0xFFFFF7E0)) {
                Row(Modifier.padding(16.dp)) {
                    Text("💡", fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(g.explain.get(), fontSize = 17.sp, lineHeight = 27.sp, color = Palette.Ink)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(t("أمثلة", "Exemples", "Examples"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink)
            Spacer(Modifier.height(8.dp))
            g.examples.forEach { ex ->
                SoftCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Ltr { Text(ex.en, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Palette.Primary, modifier = Modifier.fillMaxWidth()) }
                            ex.translation()?.let { Text(it, fontSize = 15.sp, color = Palette.Muted) }
                        }
                        Spacer(Modifier.width(8.dp))
                        SpeakButton(ex.en)
                    }
                }
            }
        }
        Box(Modifier.padding(20.dp).navigationBarsPadding()) {
            BigButton(t("فهمت، لنتمرّن!", "Compris, on s'entraîne !", "Got it, let's practise!"), onClick = onStart, color = Palette.Green)
        }
    }
}

@Composable
private fun DialoguePage(unit: LessonUnit, onStart: () -> kotlin.Unit) {
    val d = unit.dialogue
    var showTr by remember { mutableStateOf(I18n.lang != "en") }
    val speakers = d.lines.map { it.who }.distinct()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Pic(d.scene, 54.sp)
                Text(d.title.get(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { Speaker.speakAll(d.lines.map { it.en }) }, shape = RoundedCornerShape(50)) {
                        Text(t("▶ استمع للحوار كاملًا", "▶ Écouter tout le dialogue", "▶ Play the whole dialogue"))
                    }
                }
                if (I18n.lang != "en") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t("إظهار الترجمة", "Afficher la traduction", "Show translation"), color = Palette.Muted)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = showTr, onCheckedChange = { showTr = it })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Ltr {
                Column {
                    d.lines.forEach { line ->
                        val left = speakers.indexOf(line.who) % 2 == 0
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = if (left) Arrangement.Start else Arrangement.End,
                        ) {
                            Column(
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (left) Color.White else Palette.PrimaryLight)
                                    .padding(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(line.who, fontWeight = FontWeight.Bold, color = if (left) Palette.Orange else Palette.Primary, fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier.size(28.dp).clip(CircleShape).background(Palette.Primary.copy(alpha = 0.12f))
                                            .then(Modifier),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        androidx.compose.material3.IconButton(onClick = { Speaker.speak(line.en) }, modifier = Modifier.size(28.dp)) {
                                            Text("🔊", fontSize = 14.sp)
                                        }
                                    }
                                }
                                Text(line.en, fontSize = 18.sp, color = Palette.Ink)
                                if (showTr) {
                                    line.translation()?.let { tr ->
                                        CompositionLocalRtl { Text(tr, fontSize = 15.sp, color = Palette.Muted) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.padding(20.dp).navigationBarsPadding()) {
            BigButton(t("ابدأ الأسئلة", "Commencer les questions", "Start the questions"), onClick = onStart, color = Palette.Green)
        }
    }
}

/** الترجمة تتبع اتجاه لغة الواجهة */
@Composable
private fun CompositionLocalRtl(content: @Composable () -> kotlin.Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (I18n.isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
    ) { content() }
}
