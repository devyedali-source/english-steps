package com.elyedali.englishsteps

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elyedali.englishsteps.audio.Speaker
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.LessonKind
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.engine.Question
import com.elyedali.englishsteps.engine.Questions
import com.elyedali.englishsteps.i18n.I18n
import com.elyedali.englishsteps.i18n.t
import com.elyedali.englishsteps.ui.AchievementDialog
import com.elyedali.englishsteps.ui.GameScreen
import com.elyedali.englishsteps.ui.HomeActions
import com.elyedali.englishsteps.ui.HomeScreen
import com.elyedali.englishsteps.ui.LanguageScreen
import com.elyedali.englishsteps.ui.LessonIntro
import com.elyedali.englishsteps.ui.Palette
import com.elyedali.englishsteps.ui.PaywallScreen
import com.elyedali.englishsteps.ui.PlacementResultScreen
import com.elyedali.englishsteps.ui.PlacementScreen
import com.elyedali.englishsteps.ui.QuizResult
import com.elyedali.englishsteps.ui.QuizRunner
import com.elyedali.englishsteps.ui.ResultScreen
import com.elyedali.englishsteps.ui.ThemeWrap
import com.elyedali.englishsteps.ui.WelcomeScreen
import com.elyedali.englishsteps.ui.kindTitle

sealed interface Route {
    data object Language : Route
    data object Welcome : Route
    data object Placement : Route
    data class PlacementResult(val levelId: String) : Route
    data object Home : Route
    data class Lesson(val unitId: String, val kind: LessonKind) : Route
    data class LessonQuiz(val unitId: String, val kind: LessonKind) : Route
    data class Exam(val levelId: String) : Route
    data object Review : Route
    data object Daily : Route
    data class Game(val id: String) : Route
    data object Paywall : Route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.WHITE, AColor.WHITE),
        )
        I18n.init(this)
        Content.load(this)
        Progress.init(this)
        Speaker.init(this)

        val start: Route = when {
            !I18n.hasChosen(this) -> Route.Language
            !Progress.placementDone -> Route.Welcome
            else -> Route.Home
        }
        setContent {
            ThemeWrap {
                CompositionLocalProvider(LocalLayoutDirection provides if (I18n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize().background(Palette.Background).systemBarsPadding()) {
                        AppNav(start)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Progress.rollDay()
    }
}

@Composable
private fun AppNav(start: Route) {
    val stack = remember { mutableStateListOf(start) }
    var homeTab by remember { mutableIntStateOf(0) }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replace(r: Route) { stack.clear(); stack.add(r) }
    fun toHome() {
        val i = stack.indexOf(Route.Home)
        if (i < 0) replace(Route.Home) else while (stack.size > i + 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val actions = remember {
        HomeActions(
            openLesson = { u, k -> push(if (k == LessonKind.PRACTICE) Route.LessonQuiz(u.id, k) else Route.Lesson(u.id, k)) },
            openExam = { l -> push(Route.Exam(l.id)) },
            openGame = { id -> push(Route.Game(id)) },
            openDaily = { push(Route.Daily) },
            openReview = { push(Route.Review) },
            openPaywall = { push(Route.Paywall) },
        )
    }

    when (val r = stack.last()) {
        Route.Language -> LanguageScreen(onDone = { replace(if (Progress.placementDone) Route.Home else Route.Welcome) })
        Route.Welcome -> WelcomeScreen(
            onPlacement = { push(Route.Placement) },
            onBeginner = {
                Progress.finishPlacement(Content.levels.first().id)
                replace(Route.Home)
            },
        )
        Route.Placement -> PlacementScreen(onExit = { pop() }, onDone = { lvl ->
            Progress.finishPlacement(lvl.id)
            replace(Route.PlacementResult(lvl.id))
        })
        is Route.PlacementResult -> PlacementResultScreen(Content.level(r.levelId), onStart = { replace(Route.Home) })
        Route.Home -> HomeScreen(homeTab, onTab = { homeTab = it }, actions = actions)
        is Route.Lesson -> {
            val u = Content.unit(r.unitId)
            if (u == null) LaunchedEffect(r) { toHome() } else LessonIntro(u, r.kind, onBack = { pop() }, onStart = {
                pop(); push(Route.LessonQuiz(r.unitId, r.kind))
            })
        }
        is Route.LessonQuiz, is Route.Exam, Route.Review, Route.Daily -> key(r) {
            QuizHost(r, onClose = { toHome() })
        }
        is Route.Game -> key(r) { GameScreen(r.id, onExit = { pop() }) }
        Route.Paywall -> PaywallScreen(onBack = { pop() })
    }

    Progress.newAchievement?.let { id ->
        if (stack.last() !is Route.LessonQuiz && stack.last() !is Route.Exam) {
            AchievementDialog(id) { Progress.newAchievement = null }
        }
    }
}

/** ما يُعرض بعد انتهاء الأسئلة */
private data class Outcome(
    val result: QuizResult,
    val headline: String,
    val message: String,
    val emoji: String,
    val passed: Boolean,
    val bonusXp: Int,
)

@Composable
private fun QuizHost(route: Route, onClose: () -> kotlin.Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }

    val title: String = when (route) {
        is Route.LessonQuiz -> Content.unit(route.unitId)?.let { "${it.title.get()} · ${kindTitle(route.kind)}" } ?: ""
        is Route.Exam -> t("اختبار تجاوز المستوى ${route.levelId}", "Examen de passage ${route.levelId}", "${route.levelId} level test")
        Route.Review -> t("المراجعة الذكية", "Révision intelligente", "Smart review")
        else -> t("تحدي اليوم", "Défi du jour", "Daily challenge")
    }

    val out = outcome
    if (out != null) {
        ResultScreen(
            result = out.result.copy(xp = out.result.xp + out.bonusXp),
            headline = out.headline,
            message = out.message,
            emoji = out.emoji,
            buttonText = if (out.passed) t("متابعة", "Continuer", "Continue") else t("أعد المحاولة", "Réessayer", "Try again"),
            onContinue = {
                if (out.passed) onClose() else { outcome = null; attempt += 1 }
            },
            extra = {
                if (!out.passed) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text(t("رجوع للرئيسية", "Retour à l'accueil", "Back to home"))
                    }
                }
            },
        )
        return
    }

    key(attempt) {
        val questions: List<Question> = remember {
            when (route) {
                is Route.LessonQuiz -> Content.unit(route.unitId)?.let { Questions.lesson(it, route.kind) }.orEmpty()
                is Route.Exam -> Questions.levelExam(Content.level(route.levelId))
                Route.Review -> Questions.review()
                else -> Questions.dailyChallenge()
            }
        }
        QuizRunner(title = title, questions = questions, onExit = onClose, onFinish = { res ->
            outcome = applyResult(route, res)
        })
    }
}

/** يحفظ نتيجة الدرس أو الاختبار ويحسب المكافآت */
private fun applyResult(route: Route, res: QuizResult): Outcome {
    return when (route) {
        is Route.LessonQuiz -> {
            val unit = Content.unit(route.unitId)
            val passed = res.stars > 0
            val bonus = if (passed) 20 + (if (res.perfect) 30 else 0) else 0
            if (unit != null && passed) {
                val wasDone = Progress.unitDone(unit)
                Progress.saveLesson(unit.id, route.kind, res.stars, res.perfect)
                if (route.kind == LessonKind.VOCAB) Progress.learnWords(unit.vocab)
                Progress.addXp(res.xp + bonus)
                val unitNowDone = !wasDone && Progress.unitDone(unit)
                Outcome(
                    res,
                    if (unitNowDone) t("أكملت الوحدة!", "Unité terminée !", "Unit complete!") else if (res.perfect) t("بلا أي خطأ!", "Sans faute !", "Perfect!") else t("أحسنت!", "Bravo !", "Well done!"),
                    if (unitNowDone) t("فتحت الوحدة التالية. واصل هكذا!", "L'unité suivante est ouverte. Continue !", "The next unit is open. Keep going!")
                    else t("فتحت الدرس التالي.", "La leçon suivante est ouverte.", "The next lesson is open."),
                    if (unitNowDone) "🏆" else if (res.perfect) "💯" else "🎉",
                    true, bonus,
                )
            } else {
                Progress.addXp(res.xp)
                Outcome(
                    res, t("كدت تنجح!", "Presque !", "Almost there!"),
                    t("تحتاج 60% على الأقل لفتح الدرس التالي. حاول مرة أخرى، ستنجح!", "Il faut au moins 60 % pour ouvrir la leçon suivante. Réessaie, tu vas y arriver !", "You need at least 60% to open the next lesson. Try again, you can do it!"),
                    "💪", false, 0,
                )
            }
        }
        is Route.Exam -> {
            val passed = res.accuracy >= 0.8f
            if (passed) {
                Progress.passLevel(route.levelId)
                Progress.addXp(res.xp + 100)
                val next = Content.levels.getOrNull(Content.level(route.levelId).index + 1)
                Outcome(
                    res, t("نجحت في اختبار المستوى!", "Examen réussi !", "Level test passed!"),
                    if (next != null) t("انتقلت إلى المستوى ${next.id} — ${next.title.ar}", "Tu passes au niveau ${next.id} — ${next.title.fr}", "You moved up to level ${next.id} — ${next.title.en}")
                    else t("أكملت كل المستويات. أنت بطل!", "Tu as terminé tous les niveaux. Champion !", "You finished every level. Champion!"),
                    "🚀", true, 100,
                )
            } else {
                Progress.addXp(res.xp)
                Outcome(
                    res, t("لم تنجح هذه المرة", "Pas cette fois", "Not this time"),
                    t("تحتاج 80% للنجاح. راجع الوحدات ثم أعد الاختبار.", "Il faut 80 % pour réussir. Révise les unités puis recommence.", "You need 80% to pass. Review the units and try again."),
                    "📚", false, 0,
                )
            }
        }
        Route.Review -> {
            Progress.addReviewed(res.total)
            Progress.addXp(res.xp + 10)
            Outcome(res, t("أنهيت المراجعة!", "Révision terminée !", "Review done!"), t("ذاكرتك تقوى كل يوم.", "Ta mémoire se renforce chaque jour.", "Your memory gets stronger every day."), "🧠", true, 10)
        }
        else -> {
            Progress.addXp(res.xp + 20)
            Outcome(res, t("أنجزت تحدي اليوم!", "Défi du jour réussi !", "Daily challenge done!"), t("عد غدًا لتحدٍّ جديد.", "Reviens demain pour un nouveau défi.", "Come back tomorrow for a new challenge."), "⚡", true, 20)
        }
    }
}
