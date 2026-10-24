package com.elyedali.englishsteps.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.Level
import com.elyedali.englishsteps.engine.Questions
import com.elyedali.englishsteps.i18n.I18n
import com.elyedali.englishsteps.i18n.t

@Composable
fun LanguageScreen(onDone: () -> kotlin.Unit) {
    val ctx = LocalContext.current
    CenterColumn(Modifier.background(Palette.Background).statusBarsPadding()) {
        Text("🌍", fontSize = 72.sp)
        Spacer(Modifier.height(10.dp))
        Title("اختر لغة التطبيق\nChoisis la langue\nChoose your language")
        Spacer(Modifier.height(28.dp))
        I18n.languages.forEach { l ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.dp, if (I18n.lang == l.code) Palette.Primary else Palette.Locked, RoundedCornerShape(18.dp))
                    .clickable {
                        I18n.set(ctx, l.code)
                        onDone()
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(l.flag, fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Text(l.nativeName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WelcomeScreen(onPlacement: () -> kotlin.Unit, onBeginner: () -> kotlin.Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Column(
        Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("🦉", fontSize = 110.sp, modifier = Modifier.scale(scale.value))
        Spacer(Modifier.height(10.dp))
        Title("English Steps")
        Spacer(Modifier.height(8.dp))
        Sub(t("تعلّم الإنجليزية خطوة بخطوة، بالرسوم والألعاب والمحادثات، ودون إنترنت.",
            "Apprends l'anglais pas à pas, avec des dessins, des jeux et des dialogues, sans internet.",
            "Learn English step by step with pictures, games and conversations — offline."))
        Spacer(Modifier.height(24.dp))
        listOf(
            "🧭" to t("امتحان قصير يحدد مستواك", "Un court test trouve ton niveau", "A short test finds your level"),
            "🎮" to t("دروس وألعاب على مقاسك", "Leçons et jeux à ta mesure", "Lessons and games made for you"),
            "🔥" to t("نقاط وسلسلة أيام وأوسمة", "Points, séries et badges", "Points, streaks and badges"),
            "🏆" to t("نافس أفضل المتعلمين كل شهر", "Affronte les meilleurs chaque mois", "Compete with the best every month"),
        ).forEach { (e, s) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(s, fontSize = 16.sp, color = Palette.Ink)
            }
        }
        Spacer(Modifier.weight(1f))
        BigButton(t("ابدأ امتحان تحديد المستوى", "Commencer le test de niveau", "Start the placement test"), onClick = onPlacement)
        TextButton(onClick = onBeginner, modifier = Modifier.padding(top = 6.dp)) {
            Text(t("أنا مبتدئ تمامًا، ابدأ من الصفر", "Je suis grand débutant, commencer à zéro", "I'm a total beginner, start from zero"), color = Palette.Muted)
        }
    }
}

/**
 * امتحان تحديد المستوى: مرحلة لكل مستوى (6 أسئلة).
 * من يجيب على 5 أو أكثر ينتقل للمرحلة الأصعب، وإلا يتوقف عند هذا المستوى.
 */
@Composable
fun PlacementScreen(onExit: () -> kotlin.Unit, onDone: (Level) -> kotlin.Unit) {
    val levels = Content.levels
    var stage by remember { mutableIntStateOf(0) }
    var pos by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf<Boolean?>(null) }
    var between by remember { mutableStateOf(false) }
    val questions = remember(stage) { Questions.placementStage(levels[stage]) }
    val passMark = 5

    if (between) {
        CenterColumn(Modifier.background(Palette.Background)) {
            Text("🚀", fontSize = 90.sp)
            Title(t("ممتاز! مستواك أعلى من ${levels[stage - 1].id}", "Excellent ! Ton niveau dépasse ${levels[stage - 1].id}", "Excellent! You're above ${levels[stage - 1].id}"))
            Spacer(Modifier.height(8.dp))
            Sub(t("لننتقل إلى أسئلة أصعب…", "Passons à des questions plus difficiles…", "Let's try harder questions…"))
            Spacer(Modifier.height(24.dp))
            BigButton(t("متابعة", "Continuer", "Continue"), onClick = { between = false })
        }
        return
    }

    val q = questions[pos]
    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        TopBar(t("تحديد المستوى", "Test de niveau", "Placement test"), onBack = onExit, close = true) {
            Text("${levels[stage].emoji} ${levels[stage].id}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp))
        }
        SmoothProgress(
            (stage * questions.size + pos + if (answered != null) 1 else 0).toFloat() / (levels.size * questions.size),
            Modifier.padding(horizontal = 20.dp), color = Palette.Primary,
        )
        Box(Modifier.weight(1f)) {
            key(stage, pos) {
                QuestionView(q, locked = answered != null) { ok ->
                    if (ok) { correct += 1; Speaker.correct() } else Speaker.wrong()
                    answered = ok
                }
            }
        }
        if (answered != null) {
            Box(Modifier.padding(20.dp).navigationBarsPadding()) {
                BigButton(t("التالي", "Suivant", "Next"), onClick = {
                    answered = null
                    if (pos + 1 < questions.size) {
                        pos += 1
                    } else {
                        val passed = correct >= passMark
                        if (passed && stage + 1 < levels.size) {
                            stage += 1; pos = 0; correct = 0; between = true
                        } else {
                            onDone(levels[stage])
                        }
                    }
                })
            }
        }
    }
}

@Composable
fun PlacementResultScreen(level: Level, onStart: () -> kotlin.Unit) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        Speaker.success()
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    CenterColumn(Modifier.background(Palette.Background)) {
        Text("🎉", fontSize = 80.sp)
        Sub(t("مستواك هو", "Ton niveau est", "Your level is"))
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.scale(scale.value).clip(RoundedCornerShape(28.dp))
                .background(Palette.levelColors[level.index % Palette.levelColors.size])
                .padding(horizontal = 36.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(level.emoji, fontSize = 48.sp)
                Text(level.id, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(level.title.get(), fontSize = 20.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))
        Sub(t("جهزنا لك دروسًا وألعابًا وأسئلة على مقاس مستواك. أول وحدتين مجانيتان!",
            "Nous avons préparé des leçons, jeux et questions à ton niveau. Les deux premières unités sont gratuites !",
            "We've prepared lessons, games and questions at your level. The first two units are free!"), Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
        BigButton(t("لنبدأ!", "C'est parti !", "Let's go!"), onClick = onStart, color = Palette.Green)
    }
}
