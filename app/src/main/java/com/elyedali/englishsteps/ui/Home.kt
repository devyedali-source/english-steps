package com.elyedali.englishsteps.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.LessonKind
import com.elyedali.englishsteps.data.LessonUnit
import com.elyedali.englishsteps.data.Level
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.i18n.t

class HomeActions(
    val openLesson: (LessonUnit, LessonKind) -> kotlin.Unit,
    val openExam: (Level) -> kotlin.Unit,
    val openGame: (String) -> kotlin.Unit,
    val openDaily: () -> kotlin.Unit,
    val openReview: () -> kotlin.Unit,
    val openPaywall: () -> kotlin.Unit,
)

@Composable
fun HomeScreen(tab: Int, onTab: (Int) -> kotlin.Unit, actions: HomeActions) {
    Column(Modifier.fillMaxSize().background(Palette.Background)) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            when (tab) {
                0 -> LearnTab(actions)
                1 -> GamesTab(actions)
                2 -> ReviewTab(actions)
                3 -> RankingTab()
                else -> ProfileTab(actions)
            }
        }
        NavigationBar(containerColor = Color.White) {
            val items = listOf(
                "🏠" to t("تعلّم", "Apprendre", "Learn"),
                "🎮" to t("ألعاب", "Jeux", "Games"),
                "🔁" to t("مراجعة", "Révision", "Review"),
                "🏆" to t("الترتيب", "Classement", "Ranking"),
                "👤" to t("حسابي", "Profil", "Profile"),
            )
            items.forEachIndexed { i, (emoji, label) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { onTab(i) },
                    icon = {
                        Box {
                            Text(emoji, fontSize = 22.sp)
                            if (i == 2 && Progress.dueKeys().isNotEmpty()) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(Palette.Red).align(Alignment.TopEnd))
                            }
                        }
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Palette.PrimaryLight),
                )
            }
        }
    }
}

@Composable
fun StatsHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lvl = Content.level(Progress.currentLevel)
        StatPill(lvl.emoji, lvl.id, Palette.Primary)
        Spacer(Modifier.width(8.dp))
        StatPill("🔥", "${Progress.streak}", Palette.Orange)
        Spacer(Modifier.width(8.dp))
        StatPill("⭐", "${Progress.xpTotal}", Palette.Ink)
        Spacer(Modifier.weight(1f))
        GoalRing(Progress.xpToday.toFloat() / Progress.dailyGoal) {
            Text(if (Progress.goalReached) "✅" else "🎯", fontSize = 16.sp)
        }
    }
}

// ======================= تبويب التعلّم =======================

@Composable
private fun LearnTab(actions: HomeActions) {
    var selected by rememberSaveable { mutableStateOf(Progress.currentLevel) }
    var chestReward by remember { mutableIntStateOf(0) }
    val level = Content.level(selected)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StatsHeader()

        // الهدف اليومي وصندوق المكافأة
        if (Progress.chestAvailable) {
            val pulse = rememberInfiniteTransition(label = "chest")
            val s by pulse.animateFloat(1f, 1.15f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFFFFF1C9), onClick = {
                chestReward = Progress.claimChest()
            }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁", fontSize = 40.sp, modifier = Modifier.scale(s))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(t("حققت هدف اليوم!", "Objectif du jour atteint !", "Daily goal reached!"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(t("اضغط لفتح صندوق المكافأة", "Touche pour ouvrir ton coffre", "Tap to open your reward chest"), color = Palette.Muted)
                    }
                }
            }
        } else if (!Progress.goalReached) {
            SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(t("هدف اليوم", "Objectif du jour", "Daily goal"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${Progress.xpToday} / ${Progress.dailyGoal}", color = Palette.Orange, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    SmoothProgress(Progress.xpToday.toFloat() / Progress.dailyGoal, color = Palette.Orange, height = 10.dp)
                }
            }
        }

        // اختيار المستوى
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Content.levels.forEach { l ->
                val open = l.index <= Progress.levelIndex()
                val isSel = l.id == selected
                val c = Palette.levelColors[l.index % Palette.levelColors.size]
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSel) c else Color.White)
                        .border(1.5.dp, if (open) c else Palette.Locked, RoundedCornerShape(50))
                        .clickable { selected = l.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (open) l.emoji else "🔒", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("${l.id} · ${l.title.get()}", color = if (isSel) Color.White else if (open) Palette.Ink else Palette.Muted, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (level.index > Progress.levelIndex()) {
            SoftCard(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 48.sp)
                    Text(
                        t("هذا المستوى مقفل. انجح في اختبار تجاوز المستوى الحالي لفتحه.",
                            "Ce niveau est verrouillé. Réussis l'examen de passage du niveau actuel pour l'ouvrir.",
                            "This level is locked. Pass the current level test to open it."),
                        textAlign = TextAlign.Center, color = Palette.Muted,
                    )
                }
            }
        } else {
            level.units.forEach { u -> UnitCard(u, actions) }
            ExamCard(level, actions)
        }
        Spacer(Modifier.height(20.dp))
    }

    if (chestReward > 0) {
        AlertDialog(
            onDismissRequest = { chestReward = 0 },
            title = { Text("🎉 +$chestReward ⭐", fontSize = 28.sp) },
            text = { Text(t("مكافأة اليوم! عد غدًا لصندوق جديد.", "Récompense du jour ! Reviens demain pour un nouveau coffre.", "Today's reward! Come back tomorrow for a new chest.")) },
            confirmButton = { TextButton(onClick = { chestReward = 0 }) { Text(t("رائع", "Super", "Great")) } },
        )
    }
}

@Composable
private fun UnitCard(u: LessonUnit, actions: HomeActions) {
    val unlocked = Progress.unitUnlocked(u)
    val premium = Progress.unitNeedsPremium(u)
    val done = Progress.unitDone(u)
    val color = Palette.levelColors[Content.level(u.levelId).index % Palette.levelColors.size]
    SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(if (unlocked) color.copy(alpha = 0.15f) else Palette.Locked),
                    contentAlignment = Alignment.Center,
                ) { Text(if (unlocked) u.emoji else "🔒", fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("الوحدة ${u.index + 1}", "Unité ${u.index + 1}", "Unit ${u.index + 1}"), color = Palette.Muted, fontSize = 13.sp)
                    Text(u.title.get(), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (unlocked) Palette.Ink else Palette.Muted)
                }
                when {
                    premium -> Text("👑", fontSize = 22.sp)
                    done -> Text("✅", fontSize = 20.sp)
                }
            }
            if (unlocked) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    var nextMarked = false
                    LessonKind.entries.forEach { k ->
                        val open = Progress.lessonUnlocked(u, k)
                        val st = Progress.starsOf(u.id, k)
                        val isNext = open && st == 0 && !nextMarked
                        if (isNext) nextMarked = true
                        LessonNode(k, open, st, isNext, color) {
                            if (premium) actions.openPaywall() else if (open) actions.openLesson(u, k)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(t("أكمل الوحدة السابقة لفتح هذه الوحدة", "Termine l'unité précédente pour ouvrir celle-ci", "Finish the previous unit to open this one"), color = Palette.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LessonNode(kind: LessonKind, open: Boolean, stars: Int, isNext: Boolean, color: Color, onClick: () -> kotlin.Unit) {
    val pulse = rememberInfiniteTransition(label = "node")
    val s by pulse.animateFloat(1f, if (isNext) 1.1f else 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "ns")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            Modifier
                .scale(s)
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    when {
                        stars > 0 -> Brush.verticalGradient(listOf(Palette.Yellow, Palette.Orange))
                        open -> Brush.verticalGradient(listOf(color, color.copy(alpha = 0.75f)))
                        else -> Brush.verticalGradient(listOf(Palette.Locked, Palette.Locked))
                    },
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) { Text(if (open) kind.emoji else "🔒", fontSize = 24.sp) }
        Spacer(Modifier.height(4.dp))
        Text(kindTitle(kind), fontSize = 11.sp, color = Palette.Muted, textAlign = TextAlign.Center, maxLines = 1)
        StarsRow(stars, 10.sp)
    }
}

@Composable
private fun ExamCard(level: Level, actions: HomeActions) {
    val allDone = level.units.all { Progress.unitDone(it) }
    val passed = Progress.examPassed(level.id)
    SoftCard(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        color = if (passed) Palette.GreenLight else Color(0xFFEFEAFF),
        onClick = {
            when {
                !Progress.isPremium -> actions.openPaywall()
                allDone -> actions.openExam(level)
            }
        },
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (passed) "🏅" else "🎓", fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t("اختبار تجاوز المستوى ${level.id}", "Examen de passage ${level.id}", "${level.id} level test"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(
                    when {
                        passed -> t("نجحت! يمكنك إعادته متى شئت", "Réussi ! Tu peux le refaire quand tu veux", "Passed! You can retake it anytime")
                        !Progress.isPremium -> t("👑 متاح في الاشتراك المدفوع", "👑 Disponible avec l'abonnement", "👑 Available with the subscription")
                        allDone -> t("20 سؤالًا — النجاح بـ 80%", "20 questions — réussite à 80 %", "20 questions — pass at 80%")
                        else -> t("أكمل كل وحدات المستوى أولًا", "Termine d'abord toutes les unités", "Finish all units first")
                    },
                    color = Palette.Muted, fontSize = 13.sp,
                )
            }
            if (!Progress.isPremium) Text("👑", fontSize = 22.sp) else if (!allDone) Text("🔒", fontSize = 20.sp)
        }
    }
}

// ======================= تبويب الألعاب =======================

data class GameInfo(val id: String, val emoji: String, val ar: String, val fr: String, val en: String, val free: Boolean) {
    fun name() = t(ar, fr, en)
}

val Games = listOf(
    GameInfo("match", "🧩", "طابق الكلمات", "Associe les mots", "Match the words", free = true),
    GameInfo("listen", "🎧", "اسمع واختر", "Écoute et choisis", "Listen and pick", free = true),
    GameInfo("memory", "🃏", "بطاقات الذاكرة", "Cartes mémoire", "Memory cards", free = true),
    GameInfo("speed", "⏱️", "سباق الدقيقة", "Course d'une minute", "One-minute race", free = false),
    GameInfo("spell", "🔤", "صانع الكلمات", "Fabrique de mots", "Word builder", free = false),
    GameInfo("sentence", "🧱", "بنّاء الجمل", "Bâtisseur de phrases", "Sentence builder", free = false),
)

@Composable
private fun GamesTab(actions: HomeActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StatsHeader()
        SoftCard(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            color = Palette.Primary,
            onClick = actions.openDaily,
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 44.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("تحدي اليوم", "Défi du jour", "Daily challenge"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(t("10 أسئلة مختلطة مما تعلمته", "10 questions variées sur ce que tu as appris", "10 mixed questions from what you learned"), color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Games.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { g ->
                    val locked = !g.free && !Progress.isPremium
                    SoftCard(Modifier.weight(1f), onClick = { if (locked) actions.openPaywall() else actions.openGame(g.id) }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Text(g.emoji, fontSize = 46.sp)
                                if (locked) Text("👑", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(g.name(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 15.sp)
                            val best = Progress.bestScores[g.id] ?: 0
                            Text(t("أفضل نتيجة: $best", "Record : $best", "Best: $best"), color = Palette.Muted, fontSize = 12.sp)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ======================= تبويب المراجعة =======================

@Composable
private fun ReviewTab(actions: HomeActions) {
    val due = Progress.dueKeys().size
    val words = Progress.srs.entries.sortedBy { it.value.box }.take(40).mapNotNull { e -> Content.wordByKey(e.key)?.let { it to e.value.box } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        StatsHeader()
        SoftCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🧠", fontSize = 54.sp)
                Text(
                    if (due > 0) t("$due كلمة تنتظر المراجعة", "$due mot(s) à réviser", "$due word(s) to review")
                    else t("لا مراجعة مستحقة الآن", "Aucune révision pour l'instant", "Nothing due right now"),
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center,
                )
                Text(
                    t("المراجعة الذكية تعيد لك كل كلمة في الوقت المناسب قبل أن تنساها.",
                        "La révision intelligente te repropose chaque mot juste avant que tu l'oublies.",
                        "Smart review brings back each word right before you forget it."),
                    color = Palette.Muted, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                BigButton(
                    if (due > 0) t("ابدأ المراجعة", "Commencer la révision", "Start review") else t("تمرّن على كلماتك الأضعف", "Travaille tes mots faibles", "Practise your weakest words"),
                    onClick = actions.openReview,
                    color = if (due > 0) Palette.Orange else Palette.Primary,
                )
            }
        }
        if (words.isNotEmpty()) {
            Text(t("كلماتك", "Tes mots", "Your words") + " (${Progress.srs.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 20.dp))
            words.forEach { (w, box) ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Color.White)
                        .clickable { Speaker.speak(w.en) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(w.pic, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.en, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        if (w.meaning() != w.en) Text(w.meaning(), color = Palette.Muted, fontSize = 14.sp)
                    }
                    Row {
                        repeat(5) { i ->
                            Box(
                                Modifier.padding(1.dp).size(width = 6.dp, height = 16.dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (i < box) Palette.Green else Palette.Locked),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ======================= تبويب الترتيب =======================

@Composable
private fun RankingTab() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏆", fontSize = 56.sp)
        Title(t("أبطال هذا الشهر", "Champions du mois", "This month's champions"))
        Spacer(Modifier.height(16.dp))
        // منصة أفضل 3
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Podium("🥈", 90.dp, Modifier.weight(1f))
            Podium("🥇", 120.dp, Modifier.weight(1f))
            Podium("🥉", 70.dp, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SoftCard(Modifier.fillMaxWidth(), color = Palette.PrimaryLight) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👤", fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Progress.displayName.ifBlank { t("أنت", "Toi", "You") }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(t("نقاطك هذا الشهر", "Tes points ce mois-ci", "Your points this month"), color = Palette.Muted)
                }
                Text("${Progress.xpMonth} ⭐", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Palette.Primary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            t("يظهر هنا أفضل 3 متعلمين هذا الشهر ونقاط باقي المستخدمين، ويمكنك التواصل معهم عبر الواتساب. تُرسل نقاطك تلقائيًا عند اتصالك بالإنترنت.",
                "Ici apparaissent les 3 meilleurs du mois et les points des autres utilisateurs, que tu peux contacter sur WhatsApp. Tes points sont envoyés automatiquement dès que tu es connecté.",
                "The top 3 learners of the month and other users' points appear here, and you can contact them on WhatsApp. Your points are sent automatically when you are online."),
            color = Palette.Muted, textAlign = TextAlign.Center, fontSize = 14.sp,
        )
    }
}

@Composable
private fun Podium(medal: String, h: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(medal, fontSize = 34.sp)
        Text("—", color = Palette.Muted)
        Box(
            Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Brush.verticalGradient(listOf(Palette.Primary, Palette.Primary.copy(alpha = 0.6f)))),
        )
    }
}
