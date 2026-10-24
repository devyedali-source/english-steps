package com.elyedali.englishsteps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.engine.Question
import com.elyedali.englishsteps.i18n.t
import kotlinx.coroutines.delay

data class QuizResult(val correct: Int, val total: Int, val xp: Int, val maxCombo: Int) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
    val stars: Int get() = when {
        accuracy >= 0.9f -> 3
        accuracy >= 0.75f -> 2
        accuracy >= 0.6f -> 1
        else -> 0
    }
    val perfect: Boolean get() = total > 0 && correct == total
}

private val praise = listOf(
    Triple("ممتاز!", "Excellent !", "Excellent!"),
    Triple("رائع!", "Super !", "Great!"),
    Triple("أحسنت!", "Bravo !", "Well done!"),
    Triple("عبقري!", "Génial !", "Brilliant!"),
    Triple("واصل هكذا!", "Continue comme ça !", "Keep it up!"),
)

/** نص الإجابة الصحيحة لعرضه ونطقه بعد الإجابة */
private fun correctText(q: Question): String = when (q) {
    is Question.Choice -> {
        val a = q.options[q.answer]
        if (q.optionsEnglish && q.sentence != null && q.sentence.contains("___")) q.sentence.replace("___", a) else a
    }
    is Question.Order -> q.answer.joinToString(" ")
    is Question.Spell -> q.word
}

private fun spokenAnswer(q: Question): String? = when (q) {
    is Question.Choice -> when {
        q.optionsEnglish -> correctText(q)
        q.speak != null -> q.speak
        else -> null
    }
    is Question.Order -> correctText(q)
    is Question.Spell -> q.word
}

/**
 * مشغّل الأسئلة: يعرض سؤالًا بعد سؤال، يعطي نقاطًا ومضاعفة للإجابات المتتالية،
 * ويعيد السؤال الخاطئ في آخر الدرس ليتعلمه المستخدم.
 */
@Composable
fun QuizRunner(
    title: String,
    questions: List<Question>,
    onExit: () -> kotlin.Unit,
    onFinish: (QuizResult) -> kotlin.Unit,
) {
    val queue = remember { mutableStateListOf<Int>().apply { addAll(questions.indices) } }
    var pos by remember { mutableIntStateOf(0) }
    var firstCorrect by remember { mutableIntStateOf(0) }
    val attempted = remember { HashSet<Int>() }
    val requeued = remember { HashSet<Int>() }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf<Boolean?>(null) }
    var praiseText by remember { mutableStateOf("") }
    var shownOk by remember { mutableStateOf(false) }
    var shownCorrect by remember { mutableStateOf("") }
    var confirmExit by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (questions.isEmpty()) {
        LaunchedEffect(Unit) { onFinish(QuizResult(0, 0, 0, 0)) }
        return
    }

    val qi = queue.getOrNull(pos) ?: return
    val q = questions[qi]
    val doneCount = attempted.size.coerceAtMost(questions.size)

    fun handle(correct: Boolean) {
        val first = qi !in attempted
        attempted.add(qi)
        if (correct) {
            combo += 1
            if (combo > maxCombo) maxCombo = combo
            if (first) {
                firstCorrect += 1
                xp += 10 + if (combo >= 3) 5 else 0
            } else {
                xp += 3
            }
            praiseText = praise.random().let { t(it.first, it.second, it.third) }
            Speaker.correct()
        } else {
            combo = 0
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Speaker.wrong()
            if (qi !in requeued) {
                requeued.add(qi)
                queue.add(qi)
            }
        }
        if (first) q.wordKey?.let { Progress.recordWord(it, correct) }
        spokenAnswer(q)?.let { Speaker.speak(it) }
        shownOk = correct
        shownCorrect = correctText(q)
        answered = correct
    }

    fun next() {
        answered = null
        if (pos + 1 >= queue.size) {
            onFinish(QuizResult(firstCorrect, questions.size, xp, maxCombo))
        } else {
            pos += 1
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Filled.Close, contentDescription = null) }
            SmoothProgress(doneCount.toFloat() / questions.size, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            ComboBadge(combo)
            Spacer(Modifier.width(8.dp))
        }
        Text(title, color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(pos) {
                QuestionView(q = q, locked = answered != null, onAnswer = { handle(it) })
            }
        }

        AnimatedVisibility(
            visible = answered != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            val ok = shownOk
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (ok) Palette.GreenLight else Palette.RedLight)
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    if (ok) praiseText else t("الإجابة الصحيحة:", "La bonne réponse :", "Correct answer:"),
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if (ok) Palette.Green else Palette.Red,
                )
                if (!ok) {
                    Ltr { Text(shownCorrect, fontSize = 18.sp, color = Palette.Ink, modifier = Modifier.padding(top = 4.dp)) }
                } else if (combo >= 3) {
                    Text(t("🔥 $combo إجابات متتالية! نقاط مضاعفة", "🔥 $combo de suite ! Points bonus", "🔥 $combo in a row! Bonus points"), color = Palette.Orange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                BigButton(t("متابعة", "Continuer", "Continue"), onClick = { next() }, color = if (ok) Palette.Green else Palette.Red)
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(t("هل تريد الخروج؟", "Quitter ?", "Quit?")) },
            text = { Text(t("سيضيع تقدمك في هذا الدرس.", "Ta progression dans cette leçon sera perdue.", "Your progress in this lesson will be lost.")) },
            confirmButton = { TextButton(onClick = { confirmExit = false; onExit() }) { Text(t("خروج", "Quitter", "Quit"), color = Palette.Red) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(t("أكمل التعلم", "Continuer", "Keep learning")) } },
        )
    }
}

@Composable
fun ComboBadge(combo: Int) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo > 0) {
            scale.snapTo(1.4f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(scale.value)) {
        Text(if (combo >= 3) "🔥" else "⚡", fontSize = 20.sp)
        Text("$combo", fontWeight = FontWeight.ExtraBold, color = if (combo >= 3) Palette.Orange else Palette.Muted, fontSize = 18.sp)
    }
}

/** يعرض سؤالًا واحدًا بأي نوع */
@Composable
fun QuestionView(q: Question, locked: Boolean, onAnswer: (Boolean) -> kotlin.Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (q) {
            is Question.Choice -> ChoiceView(q, locked, onAnswer)
            is Question.Order -> OrderView(q.prompt, q.answer, q.tiles, " ", locked, onAnswer, header = {})
            is Question.Spell -> OrderView(
                prompt = t("اكتب الكلمة بالحروف", "Écris le mot lettre par lettre", "Spell the word"),
                answer = q.word.map { it.toString() }, tiles = q.tiles, joiner = "", locked = locked, onAnswer = onAnswer,
                header = {
                    Pic(q.pic, 84.sp)
                    if (q.meaning.isNotEmpty()) Text(q.meaning, fontSize = 18.sp, color = Palette.Muted)
                    Spacer(Modifier.height(8.dp))
                },
            )
        }
    }
}

@Composable
private fun ChoiceView(q: Question.Choice, locked: Boolean, onAnswer: (Boolean) -> kotlin.Unit) {
    var picked by remember { mutableIntStateOf(-1) }
    LaunchedEffect(q) {
        if (q.autoSpeak && q.speak != null) {
            delay(350)
            Speaker.speak(q.speak)
        }
    }
    Text(q.prompt, fontSize = 17.sp, color = Palette.Muted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    if (q.pic != null) {
        Pic(q.pic, 96.sp)
        Spacer(Modifier.height(8.dp))
    }
    if (q.sentence != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q.sentence, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Palette.Ink, modifier = Modifier.weight(1f, fill = false))
            if (q.speak != null) {
                Spacer(Modifier.width(10.dp))
                SpeakButton(q.speak)
            }
        }
    } else if (q.speak != null) {
        SpeakButton(q.speak, big = true)
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { Speaker.speak(q.speak, slow = true) }) { Text(t("🐢 نطق بطيء", "🐢 Lentement", "🐢 Slowly")) }
    }
    Spacer(Modifier.height(20.dp))

    fun colorFor(i: Int): Pair<Color, Color> = when {
        picked < 0 -> Color.White to Palette.Locked
        i == q.answer -> Palette.GreenLight to Palette.Green
        i == picked -> Palette.RedLight to Palette.Red
        else -> Color.White to Palette.Locked
    }

    fun pick(i: Int) {
        if (locked || picked >= 0) return
        picked = i
        onAnswer(i == q.answer)
    }

    if (q.optionPics) {
        q.options.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { col, opt ->
                    val i = row * 2 + col
                    val (bg, border) = colorFor(i)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .border(2.dp, border, RoundedCornerShape(20.dp))
                            .clickable { pick(i) },
                        contentAlignment = Alignment.Center,
                    ) { Pic(opt, 54.sp) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    } else {
        q.options.forEachIndexed { i, opt ->
            val (bg, border) = colorFor(i)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(2.dp, border, RoundedCornerShape(16.dp))
                    .clickable { pick(i) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(opt, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = Palette.Ink)
            }
        }
    }
}

/** ترتيب كلمات أو حروف: يضغط المستخدم على البطاقات بالترتيب الصحيح */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderView(
    prompt: String,
    answer: List<String>,
    tiles: List<String>,
    joiner: String,
    locked: Boolean,
    onAnswer: (Boolean) -> kotlin.Unit,
    header: @Composable () -> kotlin.Unit,
) {
    val chosen = remember { mutableStateListOf<Int>() }
    var result by remember { mutableStateOf<Boolean?>(null) }

    Text(prompt, fontSize = 17.sp, color = Palette.Muted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    header()

    Ltr {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // منطقة الجواب
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        when (result) {
                            true -> Palette.GreenLight
                            false -> Palette.RedLight
                            null -> Color.White
                        },
                    )
                    .border(2.dp, Palette.Locked, RoundedCornerShape(18.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (chosen.isEmpty()) {
                    Text("· · ·", color = Palette.Muted, fontSize = 22.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chosen.forEach { idx ->
                        Tile(tiles[idx], used = false) { if (!locked && result == null) chosen.remove(idx) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                tiles.forEachIndexed { idx, s ->
                    val used = idx in chosen
                    Tile(s, used = used) {
                        if (!used && !locked && result == null && chosen.size < answer.size) chosen.add(idx)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (joiner.isEmpty() && result == null) {
                IconButton(onClick = { if (chosen.isNotEmpty() && result == null) chosen.removeAt(chosen.lastIndex) }) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = Palette.Muted)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (result == null) {
        BigButton(
            t("تحقق", "Vérifier", "Check"),
            enabled = chosen.size == answer.size && !locked,
            onClick = {
                val built = chosen.map { tiles[it] }
                val ok = built.joinToString(joiner).equals(answer.joinToString(joiner), ignoreCase = true)
                result = ok
                onAnswer(ok)
            },
        )
    }
}

@Composable
private fun Tile(text: String, used: Boolean, onClick: () -> kotlin.Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (used) Palette.Locked else Color.White)
            .border(2.dp, if (used) Palette.Locked else Palette.Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(enabled = !used) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = if (used) Color.Transparent else Palette.Ink)
    }
}

/** شاشة نتيجة الدرس */
@Composable
fun ResultScreen(
    result: QuizResult,
    headline: String,
    message: String,
    emoji: String,
    buttonText: String,
    onContinue: () -> kotlin.Unit,
    extra: @Composable () -> kotlin.Unit = {},
) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        Speaker.success()
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Column(
        Modifier.fillMaxSize().background(Palette.Background).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(30.dp))
        Text(emoji, fontSize = 90.sp, modifier = Modifier.scale(scale.value))
        Spacer(Modifier.height(10.dp))
        Title(headline)
        Spacer(Modifier.height(6.dp))
        Sub(message)
        Spacer(Modifier.height(16.dp))
        if (result.total > 0) StarsRow(result.stars, 40.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultTile("🎯", "${(result.accuracy * 100).toInt()}%", t("الدقة", "Précision", "Accuracy"), Modifier.weight(1f))
            ResultTile("⭐", "+${result.xp}", t("نقاط", "Points", "Points"), Modifier.weight(1f))
            ResultTile("🔥", "${result.maxCombo}", t("أطول سلسلة", "Meilleure série", "Best combo"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        extra()
        Spacer(Modifier.height(20.dp))
        BigButton(buttonText, onClick = onContinue)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun ResultTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Palette.Ink)
            Text(label, fontSize = 12.sp, color = Palette.Muted, textAlign = TextAlign.Center)
        }
    }
}
