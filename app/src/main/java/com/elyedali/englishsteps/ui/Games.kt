package com.elyedali.englishsteps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.data.Word
import com.elyedali.englishsteps.engine.Question
import com.elyedali.englishsteps.engine.Questions
import com.elyedali.englishsteps.i18n.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** كلمات اللعب: ما تعلّمه المستخدم، دون تكرار في الرسوم */
private fun gameWords(n: Int): List<Word> {
    val learned = Questions.learnedWords().distinctBy { it.pic }.distinctBy { it.en.lowercase() }.shuffled()
    if (learned.size >= n) return learned.take(n)
    val extra = Content.level(Progress.currentLevel).units.flatMap { it.vocab }
    return (learned + extra.shuffled()).distinctBy { it.pic }.distinctBy { it.en.lowercase() }.take(n)
}

@Composable
fun GameScreen(gameId: String, onExit: () -> kotlin.Unit) {
    var round by remember { mutableIntStateOf(0) }
    var finalScore by remember { mutableStateOf<Int?>(null) }
    val info = Games.firstOrNull { it.id == gameId } ?: Games.first()

    val score = finalScore
    if (score != null) {
        GameOver(info, score, onAgain = { finalScore = null; round += 1 }, onExit = onExit)
        return
    }
    key(round) {
        val finish: (Int) -> kotlin.Unit = { s -> finalScore = s }
        when (gameId) {
            "match" -> MatchGame(info, onExit, finish)
            "memory" -> MemoryGame(info, onExit, finish)
            "speed" -> SpeedGame(info, onExit, finish)
            "listen" -> {
                val qs = remember { gameWords(10).let { ws -> ws.map { Questions.listenToPic(it, ws) } } }
                QuestionGame(info, qs, 10, onExit, finish)
            }
            "spell" -> {
                val qs = remember {
                    val ws = Questions.learnedWords().filter { Questions.canSpell(it) }.shuffled().take(8)
                    ws.map { Questions.spell(it) }
                }
                QuestionGame(info, qs, 15, onExit, finish)
            }
            else -> {
                val qs = remember {
                    val units = Questions.learnedWords().map { it.unitId }.distinct().mapNotNull { Content.unit(it) }
                    units.flatMap { it.grammar.order }.shuffled().take(6).map { Questions.order(it) }
                }
                QuestionGame(info, qs, 20, onExit, finish)
            }
        }
    }
}

@Composable
private fun GameHeader(info: GameInfo, score: Int, onExit: () -> kotlin.Unit, right: @Composable () -> kotlin.Unit = {}) {
    TopBar("${info.emoji} ${info.name()}", onBack = onExit, close = true) {
        right()
        Spacer(Modifier.width(8.dp))
        StatPill("⭐", "$score", Palette.Primary)
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun Hearts(lives: Int, max: Int = 3) {
    Row { repeat(max) { i -> Text(if (i < lives) "❤️" else "🤍", fontSize = 18.sp) } }
}

// ---------- 1) طابق الكلمات ----------

@Composable
private fun MatchGame(info: GameInfo, onExit: () -> kotlin.Unit, onFinish: (Int) -> kotlin.Unit) {
    val totalRounds = 3
    var round by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    val words = remember(round) { gameWords(5) }
    val pics = remember(round) { words.shuffled() }
    val matched = remember(round) { mutableStateListOf<String>() }
    var selected by remember(round) { mutableStateOf<String?>(null) }
    var wrongPic by remember(round) { mutableStateOf<String?>(null) }

    LaunchedEffect(matched.size, round) {
        if (words.isNotEmpty() && matched.size == words.size) {
            delay(500)
            if (round + 1 >= totalRounds) onFinish(score) else round += 1
        }
    }
    LaunchedEffect(wrongPic) { if (wrongPic != null) { delay(500); wrongPic = null } }

    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        GameHeader(info, score, onExit) { Text("${round + 1}/$totalRounds", color = Palette.Muted) }
        Sub(t("اضغط على الكلمة ثم على رسمها", "Touche un mot puis son dessin", "Tap a word, then its picture"), Modifier.fillMaxWidth().padding(8.dp))
        Ltr {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    words.forEach { w ->
                        val done = w.key in matched
                        val sel = selected == w.key
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (done) Palette.GreenLight else if (sel) Palette.PrimaryLight else Color.White)
                                .border(2.dp, if (sel) Palette.Primary else if (done) Palette.Green else Palette.Locked, RoundedCornerShape(16.dp))
                                .clickable(enabled = !done) { selected = w.key; Speaker.speak(w.en) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(w.en, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (done) Palette.Green else Palette.Ink, textAlign = TextAlign.Center) }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pics.forEach { w ->
                        val done = w.key in matched
                        val wrong = wrongPic == w.key
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (done) Palette.GreenLight else if (wrong) Palette.RedLight else Color.White)
                                .border(2.dp, if (done) Palette.Green else if (wrong) Palette.Red else Palette.Locked, RoundedCornerShape(16.dp))
                                .clickable(enabled = !done) {
                                    val s = selected ?: return@clickable
                                    if (s == w.key) {
                                        matched.add(w.key); score += 10; Speaker.correct()
                                        Progress.recordWord(w.key, true)
                                    } else {
                                        wrongPic = w.key; score = (score - 3).coerceAtLeast(0); Speaker.wrong()
                                    }
                                    selected = null
                                },
                            contentAlignment = Alignment.Center,
                        ) { Pic(w.pic, 34.sp) }
                    }
                }
            }
        }
    }
}

// ---------- 2) بطاقات الذاكرة ----------

private data class MemCard(val id: Int, val key: String, val face: String, val isPic: Boolean)

@Composable
private fun MemoryGame(info: GameInfo, onExit: () -> kotlin.Unit, onFinish: (Int) -> kotlin.Unit) {
    val words = remember { gameWords(6) }
    val cards = remember {
        words.flatMap { listOf(it.key to it.en, it.key to it.pic) }
            .mapIndexed { i, (k, f) -> MemCard(i, k, f, isPic = i % 2 == 1) }
            .shuffled()
    }
    val open = remember { mutableStateListOf<Int>() }
    val solved = remember { mutableStateListOf<String>() }
    var moves by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val score = (130 - moves * 5).coerceAtLeast(20)

    LaunchedEffect(solved.size) {
        if (words.isNotEmpty() && solved.size == words.size) { delay(600); onFinish(score) }
    }

    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        GameHeader(info, score, onExit) { Text(t("محاولات: $moves", "Coups : $moves", "Moves: $moves"), color = Palette.Muted) }
        Sub(t("اقلب بطاقتين: كلمة ورسمها", "Retourne deux cartes : un mot et son dessin", "Flip two cards: a word and its picture"), Modifier.fillMaxWidth().padding(8.dp))
        Ltr {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c ->
                            val faceUp = c.id in open || c.key in solved
                            val rot = remember { Animatable(0f) }
                            LaunchedEffect(faceUp) { rot.animateTo(if (faceUp) 180f else 0f, tween(250)) }
                            Box(
                                Modifier.weight(1f).aspectRatio(0.95f)
                                    .graphicsLayer { rotationY = rot.value; cameraDistance = 12f * density }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            c.key in solved -> Palette.GreenLight
                                            rot.value > 90f -> Color.White
                                            else -> Palette.Primary
                                        },
                                    )
                                    .border(2.dp, if (c.key in solved) Palette.Green else Palette.Locked, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !faceUp && !busy) {
                                        open.add(c.id)
                                        if (!c.isPic) Speaker.speak(c.face)
                                        if (open.size == 2) {
                                            moves += 1
                                            val a = cards.first { it.id == open[0] }
                                            val b = cards.first { it.id == open[1] }
                                            if (a.key == b.key) {
                                                solved.add(a.key); open.clear(); Speaker.correct()
                                                Progress.recordWord(a.key, true)
                                            } else {
                                                busy = true
                                                scope.launch { delay(800); open.clear(); busy = false }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (rot.value > 90f) {
                                    Box(Modifier.graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
                                        if (c.isPic) Pic(c.face, 40.sp)
                                        else Text(c.face, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp))
                                    }
                                } else {
                                    Text("❓", fontSize = 28.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- 3) سباق الدقيقة ----------

@Composable
private fun SpeedGame(info: GameInfo, onExit: () -> kotlin.Unit, onFinish: (Int) -> kotlin.Unit) {
    val words = remember { gameWords(20).ifEmpty { Content.allWords().take(20) } }
    var timeLeft by remember { mutableIntStateOf(60) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf<Boolean?>(null) }

    fun newRound(): Pair<Word, Word> {
        val w = words.random()
        val shown = if (Math.random() < 0.5) w else words.filter { it.pic != w.pic }.randomOrNull() ?: w
        return w to shown
    }
    var pair by remember { mutableStateOf(newRound()) }

    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        while (timeLeft > 0) { delay(1000); timeLeft -= 1 }
        onFinish(score)
    }
    LaunchedEffect(flash) { if (flash != null) { delay(250); flash = null } }

    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        GameHeader(info, score, onExit) { Text("⏱️ $timeLeft", fontWeight = FontWeight.Bold, color = if (timeLeft <= 10) Palette.Red else Palette.Ink) }
        if (!started) {
            CenterColumn {
                Text("⏱️", fontSize = 80.sp)
                Title(t("هل الكلمة تطابق الرسم؟", "Le mot correspond-il au dessin ?", "Does the word match the picture?"))
                Sub(t("أجب بأسرع ما يمكن خلال 60 ثانية. الإجابات المتتالية تضاعف نقاطك!", "Réponds le plus vite possible en 60 secondes. Les séries multiplient tes points !", "Answer as fast as you can in 60 seconds. Streaks multiply your points!"))
                Spacer(Modifier.height(24.dp))
                BigButton(t("انطلق!", "Partez !", "Go!"), onClick = { started = true }, color = Palette.Orange)
            }
            return@Column
        }
        val mult = 1 + combo / 5
        SmoothProgress(timeLeft / 60f, Modifier.padding(horizontal = 20.dp), color = if (timeLeft <= 10) Palette.Red else Palette.Orange)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(when (flash) { true -> Palette.GreenLight; false -> Palette.RedLight; null -> Color.White }),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Pic(pair.first.pic, 110.sp)
            Spacer(Modifier.height(10.dp))
            Text(pair.second.en, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            if (mult > 1) Text("🔥 ×$mult", color = Palette.Orange, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        fun answer(saysYes: Boolean) {
            val truth = pair.first.key == pair.second.key
            if (saysYes == truth) {
                score += 10 * mult; combo += 1; flash = true; Speaker.correct()
            } else {
                combo = 0; flash = false; Speaker.wrong()
            }
            pair = newRound()
        }
        Row(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton("✗", onClick = { answer(false) }, modifier = Modifier.weight(1f), color = Palette.Red)
            BigButton("✓", onClick = { answer(true) }, modifier = Modifier.weight(1f), color = Palette.Green)
        }
    }
}

// ---------- 4) ألعاب الأسئلة: اسمع واختر، صانع الكلمات، بنّاء الجمل ----------

@Composable
private fun QuestionGame(info: GameInfo, questions: List<Question>, points: Int, onExit: () -> kotlin.Unit, onFinish: (Int) -> kotlin.Unit) {
    var pos by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var answered by remember { mutableStateOf<Boolean?>(null) }

    if (questions.isEmpty()) {
        LaunchedEffect(Unit) { onFinish(0) }
        return
    }
    LaunchedEffect(answered) {
        val a = answered ?: return@LaunchedEffect
        delay(if (a) 900 else 1600)
        answered = null
        if (lives <= 0 || pos + 1 >= questions.size) onFinish(score) else pos += 1
    }

    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        GameHeader(info, score, onExit) { Hearts(lives) }
        SmoothProgress((pos + 1f) / questions.size, Modifier.padding(horizontal = 20.dp), color = Palette.Primary, height = 10.dp)
        Box(Modifier.weight(1f)) {
            key(pos) {
                QuestionView(questions[pos], locked = answered != null) { ok ->
                    if (ok) { score += points; Speaker.correct() } else { lives -= 1; Speaker.wrong() }
                    questions[pos].wordKey?.let { Progress.recordWord(it, ok) }
                    answered = ok
                }
            }
        }
        val a = answered
        if (a == false) {
            val q = questions[pos]
            val correct = when (q) {
                is Question.Choice -> q.options[q.answer]
                is Question.Order -> q.answer.joinToString(" ")
                is Question.Spell -> q.word
            }
            Box(Modifier.fillMaxWidth().background(Palette.RedLight).padding(16.dp).navigationBarsPadding()) {
                Ltr { Text("✔ $correct", fontSize = if (q is Question.Choice && q.optionPics) 36.sp else 20.sp, fontWeight = FontWeight.Bold, color = Palette.Ink) }
            }
        }
    }
}

// ---------- نهاية اللعبة ----------

@Composable
private fun GameOver(info: GameInfo, score: Int, onAgain: () -> kotlin.Unit, onExit: () -> kotlin.Unit) {
    var record by remember { mutableStateOf(false) }
    var xp by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        record = Progress.gameFinished(info.id, score)
        xp = (score / 2).coerceAtMost(80)
        Progress.addXp(xp)
        Speaker.success()
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    CenterColumn(Modifier.background(Palette.Background)) {
        Text(if (record) "🏆" else info.emoji, fontSize = 90.sp, modifier = Modifier.scale(scale.value))
        Title(if (record) t("رقم قياسي جديد!", "Nouveau record !", "New record!") else t("انتهت اللعبة", "Partie terminée", "Game over"))
        Spacer(Modifier.height(10.dp))
        Text("$score", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Primary)
        Sub(t("أفضل نتيجة: ${Progress.bestScores[info.id] ?: score}", "Record : ${Progress.bestScores[info.id] ?: score}", "Best: ${Progress.bestScores[info.id] ?: score}"))
        Spacer(Modifier.height(6.dp))
        Text("+$xp ⭐", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Orange)
        Spacer(Modifier.height(28.dp))
        BigButton(t("العب مرة أخرى", "Rejouer", "Play again"), onClick = onAgain, color = Palette.Orange)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Text(t("رجوع", "Retour", "Back"), fontSize = 17.sp)
        }
    }
}
