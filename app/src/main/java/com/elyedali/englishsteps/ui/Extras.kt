package com.elyedali.englishsteps.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elyedali.englishsteps.data.Achievements
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.LessonKind
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.i18n.I18n
import com.elyedali.englishsteps.i18n.t

object Plan {
    const val PRICE = 300
    const val DAYS = 45
}

// ======================= حسابي =======================

@Composable
fun ProfileTab(actions: HomeActions) {
    val ctx = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    val lessonsDone = Progress.stars.values.count { it > 0 }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Palette.Primary, Palette.Orange))),
                contentAlignment = Alignment.Center,
            ) { Text("🦉", fontSize = 38.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Progress.displayName.ifBlank { t("متعلم جديد", "Nouvel apprenant", "New learner") },
                    fontWeight = FontWeight.ExtraBold, fontSize = 21.sp,
                )
                val lvl = Content.level(Progress.currentLevel)
                Text("${lvl.emoji} ${lvl.id} · ${lvl.title.get()}", color = Palette.Muted)
            }
            TextButton(onClick = { editName = true }) { Text(t("تعديل", "Modifier", "Edit")) }
        }
        Spacer(Modifier.height(16.dp))

        // الاشتراك
        SoftCard(Modifier.fillMaxWidth(), color = if (Progress.isPremium) Palette.GreenLight else Color(0xFFFFF1C9), onClick = actions.openPaywall) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (Progress.isPremium) {
                        Text(t("الاشتراك مفعّل", "Abonnement actif", "Subscription active"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(t("باقٍ ${Progress.daysLeft()} يومًا", "Encore ${Progress.daysLeft()} jours", "${Progress.daysLeft()} days left"), color = Palette.Muted)
                    } else {
                        Text(t("افتح كل الدروس والألعاب", "Débloque toutes les leçons et jeux", "Unlock all lessons and games"), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(t("${Plan.PRICE} أوقية لمدة ${Plan.DAYS} يومًا", "${Plan.PRICE} ouguiyas pour ${Plan.DAYS} jours", "${Plan.PRICE} MRU for ${Plan.DAYS} days"), color = Palette.Muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // الإحصاءات
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultTile("⭐", "${Progress.xpTotal}", t("كل النقاط", "Points totaux", "Total points"), Modifier.weight(1f))
            ResultTile("🔥", "${Progress.bestStreak}", t("أطول سلسلة", "Meilleure série", "Best streak"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultTile("📚", "${Progress.srs.size}", t("كلمات تعلّمتها", "Mots appris", "Words learned"), Modifier.weight(1f))
            ResultTile("✅", "$lessonsDone", t("دروس أنجزتها", "Leçons faites", "Lessons done"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        // الأوسمة
        Text(t("أوسمتي", "Mes badges", "My badges") + " (${Achievements.all.count { Progress.achievements.containsKey(it.id) }}/${Achievements.all.size})",
            fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Achievements.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    val got = Progress.achievements.containsKey(a.id)
                    SoftCard(Modifier.weight(1f), color = if (got) Color.White else Color(0xFFEDECF4)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (got) a.emoji else "🔒", fontSize = 30.sp)
                            Text(t(a.ar, a.fr, a.en), fontSize = 12.sp, textAlign = TextAlign.Center, color = if (got) Palette.Ink else Palette.Muted, maxLines = 2)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(20.dp))

        // الإعدادات
        Text(t("الإعدادات", "Paramètres", "Settings"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(t("لغة التطبيق", "Langue de l'application", "App language"), color = Palette.Muted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    I18n.languages.forEach { l ->
                        val sel = I18n.lang == l.code
                        Text(
                            "${l.flag} ${l.nativeName}",
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) Palette.Primary else Color.White)
                                .border(1.dp, if (sel) Palette.Primary else Palette.Locked, RoundedCornerShape(50))
                                .clickable { I18n.set(ctx, l.code) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (sel) Color.White else Palette.Ink, fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(t("هدفي اليومي", "Mon objectif quotidien", "My daily goal"), color = Palette.Muted)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        30 to t("خفيف", "Léger", "Light"),
                        50 to t("عادي", "Normal", "Regular"),
                        100 to t("جاد", "Sérieux", "Serious"),
                        200 to t("بطل", "Champion", "Champion"),
                    ).forEach { (v, label) ->
                        val sel = Progress.dailyGoal == v
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) Palette.Orange else Color.White)
                                .border(1.dp, if (sel) Palette.Orange else Palette.Locked, RoundedCornerShape(14.dp))
                                .clickable { Progress.setDailyGoal(v) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("$v", fontWeight = FontWeight.Bold, color = if (sel) Color.White else Palette.Ink)
                            Text(label, fontSize = 11.sp, color = if (sel) Color.White else Palette.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("🔊 أصوات الإجابات", "🔊 Sons des réponses", "🔊 Answer sounds"), modifier = Modifier.weight(1f))
                    Switch(checked = Progress.soundOn, onCheckedChange = { Progress.setSound(it) })
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    if (editName) {
        var name by remember { mutableStateOf(Progress.displayName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(t("اسمك الظاهر للآخرين", "Ton nom visible par les autres", "Your name shown to others")) },
            text = { OutlinedTextField(value = name, onValueChange = { if (it.length <= 24) name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { Progress.setName(name); editName = false }) { Text(t("حفظ", "Enregistrer", "Save")) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(t("إلغاء", "Annuler", "Cancel")) } },
        )
    }
}

// ======================= الاشتراك =======================

@Composable
fun PaywallScreen(onBack: () -> kotlin.Unit) {
    val ctx = LocalContext.current
    val totalLessons = Content.levels.sumOf { it.units.size } * LessonKind.entries.size
    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        TopBar("", onBack = onBack, close = true)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👑", fontSize = 80.sp)
            Title(t("English Steps الكامل", "English Steps complet", "English Steps Full"))
            Spacer(Modifier.height(6.dp))
            Sub(t("كل ما تحتاجه لتتقن الإنجليزية", "Tout pour maîtriser l'anglais", "Everything you need to master English"))
            Spacer(Modifier.height(20.dp))
            listOf(
                "📚" to t("كل الوحدات في كل المستويات ($totalLessons درسًا)", "Toutes les unités de tous les niveaux ($totalLessons leçons)", "All units in every level ($totalLessons lessons)"),
                "🎓" to t("اختبار تجاوز المستوى والانتقال للمستوى التالي", "Examen de passage et accès au niveau suivant", "Level tests to move up to the next level"),
                "🎮" to t("كل الألعاب الستة", "Les six jeux", "All six games"),
                "💬" to t("كل المحادثات والقواعد مع النطق", "Tous les dialogues et la grammaire avec prononciation", "All conversations and grammar with audio"),
                "🧠" to t("مراجعة ذكية لكل كلماتك", "Révision intelligente de tous tes mots", "Smart review of all your words"),
                "🔁" to t("عند التجديد تواصل من حيث توقفت", "Au renouvellement, tu reprends là où tu t'es arrêté", "When you renew, you continue where you stopped"),
            ).forEach { (e, s) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(s, fontSize = 16.sp, color = Palette.Ink)
                }
            }
            Spacer(Modifier.height(20.dp))
            SoftCard(Modifier.fillMaxWidth(), color = Palette.Primary) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t("${Plan.PRICE} أوقية", "${Plan.PRICE} ouguiyas", "${Plan.PRICE} MRU"), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(t("لمدة ${Plan.DAYS} يومًا", "pour ${Plan.DAYS} jours", "for ${Plan.DAYS} days"), color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            BigButton(
                t("اطلب التفعيل عبر واتساب", "Demander l'activation via WhatsApp", "Request activation via WhatsApp"),
                color = Color(0xFF25D366),
                onClick = {
                    // يُربط بتسجيل الدخول ورابط التفعيل في مرحلة Firebase
                    Toast.makeText(ctx, t("التفعيل عبر واتساب يُربط في المرحلة القادمة", "L'activation WhatsApp sera reliée à l'étape suivante", "WhatsApp activation will be connected in the next step"), Toast.LENGTH_LONG).show()
                },
            )
        }
    }
}

/** احتفال بوسام جديد */
@Composable
fun AchievementDialog(id: String, onDismiss: () -> kotlin.Unit) {
    val a = Achievements.all.firstOrNull { it.id == id } ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(a.emoji, fontSize = 64.sp)
                Text(t("وسام جديد!", "Nouveau badge !", "New badge!"), fontWeight = FontWeight.ExtraBold)
            }
        },
        text = { Text(t(a.ar, a.fr, a.en), fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(t("رائع!", "Génial !", "Awesome!")) } },
    )
}
