package com.elyedali.englishsteps.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/** صندوق المراجعة الذكية لكل كلمة (خوارزمية لايتنر) */
data class SrsItem(val box: Int, val dueDay: Long)

/**
 * تقدّم المستخدم كله محفوظ في الهاتف ويعمل دون إنترنت.
 * عند انتهاء الاشتراك وتجديده يبقى كل شيء كما هو، فيواصل من حيث توقف.
 */
object Progress {
    private lateinit var sp: SharedPreferences

    var placementDone by mutableStateOf(false)
        private set
    /** أعلى مستوى وصل إليه المستخدم */
    var currentLevel by mutableStateOf("A1")
        private set
    var xpTotal by mutableIntStateOf(0)
        private set
    var xpMonth by mutableIntStateOf(0)
        private set
    var xpToday by mutableIntStateOf(0)
        private set
    var streak by mutableIntStateOf(0)
        private set
    var bestStreak by mutableIntStateOf(0)
        private set
    var dailyGoal by mutableIntStateOf(50)
        private set
    var chestClaimedDay by mutableStateOf(-1L)
        private set
    var soundOn by mutableStateOf(true)
        private set
    var displayName by mutableStateOf("")
        private set
    var gamesPlayed by mutableIntStateOf(0)
        private set
    var perfectCount by mutableIntStateOf(0)
        private set
    var reviewedCount by mutableIntStateOf(0)
        private set
    /** نهاية الاشتراك المدفوع (0 = غير مشترك). يُفعَّل لاحقًا عبر رابط الواتساب */
    var premiumUntil by mutableStateOf(0L)
        private set

    private var monthKey = 0
    private var todayKey = 0L
    private var lastActiveDay = -1L

    /** نجوم كل درس: المفتاح "unitId:kind" */
    val stars = mutableStateMapOf<String, Int>()
    val srs = mutableStateMapOf<String, SrsItem>()
    val achievements = mutableStateMapOf<String, Long>()
    val bestScores = mutableStateMapOf<String, Int>()

    fun init(context: Context) {
        sp = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
        placementDone = sp.getBoolean("placementDone", false)
        currentLevel = sp.getString("currentLevel", "A1") ?: "A1"
        xpTotal = sp.getInt("xpTotal", 0)
        xpMonth = sp.getInt("xpMonth", 0)
        monthKey = sp.getInt("monthKey", 0)
        xpToday = sp.getInt("xpToday", 0)
        todayKey = sp.getLong("todayKey", 0L)
        streak = sp.getInt("streak", 0)
        bestStreak = sp.getInt("bestStreak", 0)
        lastActiveDay = sp.getLong("lastActiveDay", -1L)
        dailyGoal = sp.getInt("dailyGoal", 50)
        chestClaimedDay = sp.getLong("chestClaimedDay", -1L)
        soundOn = sp.getBoolean("soundOn", true)
        displayName = sp.getString("displayName", "") ?: ""
        gamesPlayed = sp.getInt("gamesPlayed", 0)
        perfectCount = sp.getInt("perfectCount", 0)
        reviewedCount = sp.getInt("reviewedCount", 0)
        premiumUntil = sp.getLong("premiumUntil", 0L)
        readIntMap("stars").forEach { (k, v) -> stars[k] = v }
        readIntMap("bestScores").forEach { (k, v) -> bestScores[k] = v }
        readJson("achievements").let { o -> o.keys().forEach { achievements[it] = o.optLong(it) } }
        readJson("srs").let { o ->
            o.keys().forEach { k ->
                val v = o.optString(k)
                val box = v.substringBefore(',').toIntOrNull() ?: 1
                val due = v.substringAfter(',').toLongOrNull() ?: 0L
                srs[k] = SrsItem(box, due)
            }
        }
        rollDay()
    }

    // ---------- الوقت ----------

    fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    private fun currentMonthKey(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1
    }

    /** يصفّر نقاط اليوم ونقاط الشهر عند تغيّرهما، ويقطع السلسلة إن فات يوم دون تعلّم */
    fun rollDay() {
        val d = today()
        val m = currentMonthKey()
        val e = sp.edit()
        if (todayKey != d) {
            todayKey = d; xpToday = 0
            e.putLong("todayKey", d).putInt("xpToday", 0)
        }
        if (monthKey != m) {
            monthKey = m; xpMonth = 0
            e.putInt("monthKey", m).putInt("xpMonth", 0)
        }
        if (lastActiveDay >= 0 && d - lastActiveDay > 1 && streak != 0) {
            streak = 0
            e.putInt("streak", 0)
        }
        e.apply()
    }

    // ---------- النقاط والسلسلة ----------

    fun addXp(amount: Int) {
        if (amount <= 0) return
        rollDay()
        xpTotal += amount; xpMonth += amount; xpToday += amount
        val d = today()
        if (lastActiveDay != d) {
            streak = if (lastActiveDay == d - 1) streak + 1 else 1
            lastActiveDay = d
            if (streak > bestStreak) bestStreak = streak
        }
        sp.edit()
            .putInt("xpTotal", xpTotal).putInt("xpMonth", xpMonth).putInt("xpToday", xpToday)
            .putInt("streak", streak).putInt("bestStreak", bestStreak).putLong("lastActiveDay", lastActiveDay)
            .apply()
        checkAchievements()
    }

    val goalReached: Boolean get() = xpToday >= dailyGoal
    val chestAvailable: Boolean get() = goalReached && chestClaimedDay != today()

    /** صندوق المكافأة اليومي: نقاط عشوائية مرة واحدة في اليوم بعد بلوغ الهدف */
    fun claimChest(): Int {
        if (!chestAvailable) return 0
        val reward = listOf(15, 20, 25, 30, 40, 50).random()
        chestClaimedDay = today()
        sp.edit().putLong("chestClaimedDay", chestClaimedDay).apply()
        addXp(reward)
        return reward
    }

    fun setDailyGoal(v: Int) { dailyGoal = v; sp.edit().putInt("dailyGoal", v).apply() }
    fun setSound(on: Boolean) { soundOn = on; sp.edit().putBoolean("soundOn", on).apply() }
    fun setName(name: String) { displayName = name.trim(); sp.edit().putString("displayName", displayName).apply() }

    // ---------- المستوى والاشتراك ----------

    val isPremium: Boolean get() = System.currentTimeMillis() < premiumUntil

    fun daysLeft(): Int = if (!isPremium) 0 else (((premiumUntil - System.currentTimeMillis()) / 86_400_000L) + 1).toInt()

    /** يُستدعى عند وصول تفعيل الاشتراك (45 يومًا) */
    fun setPremiumUntil(ms: Long) { premiumUntil = ms; sp.edit().putLong("premiumUntil", ms).apply() }

    fun finishPlacement(levelId: String) {
        placementDone = true
        currentLevel = levelId
        sp.edit().putBoolean("placementDone", true).putString("currentLevel", levelId).apply()
        unlock("placement")
    }

    fun levelIndex(): Int = Content.levels.indexOfFirst { it.id == currentLevel }.coerceAtLeast(0)

    /** نجاح اختبار تجاوز المستوى: ينتقل المستخدم للمستوى التالي */
    fun passLevel(levelId: String) {
        unlock("exam_$levelId")
        unlock("level_up")
        val idx = Content.levels.indexOfFirst { it.id == levelId }
        val next = Content.levels.getOrNull(idx + 1) ?: return
        if (next.index > levelIndex()) {
            currentLevel = next.id
            sp.edit().putString("currentLevel", next.id).apply()
        }
    }

    fun examPassed(levelId: String): Boolean = achievements.containsKey("exam_$levelId")

    // ---------- الدروس ----------

    fun starsOf(unitId: String, kind: LessonKind): Int = stars["$unitId:${kind.id}"] ?: 0

    fun saveLesson(unitId: String, kind: LessonKind, newStars: Int, perfect: Boolean) {
        val k = "$unitId:${kind.id}"
        if (newStars > (stars[k] ?: 0)) stars[k] = newStars
        if (perfect) {
            perfectCount += 1
            sp.edit().putInt("perfectCount", perfectCount).apply()
        }
        writeIntMap("stars", stars)
        unlock("first_lesson")
        checkAchievements()
    }

    fun unitDone(unit: LessonUnit): Boolean = LessonKind.entries.all { starsOf(unit.id, it) > 0 }

    fun unitStars(unit: LessonUnit): Int = LessonKind.entries.sumOf { starsOf(unit.id, it) }

    /** هل الوحدة مفتوحة؟ المستويات السابقة مفتوحة كلها، وفي المستوى الحالي تُفتح الوحدات بالتتابع */
    fun unitUnlocked(unit: LessonUnit): Boolean {
        val lvl = Content.level(unit.levelId)
        if (lvl.index > levelIndex()) return false
        if (lvl.index < levelIndex()) return true
        if (unit.index == 0) return true
        return unitDone(lvl.units[unit.index - 1])
    }

    fun unitNeedsPremium(unit: LessonUnit): Boolean = unit.index >= Content.FREE_UNITS && !isPremium

    fun lessonUnlocked(unit: LessonUnit, kind: LessonKind): Boolean {
        if (!unitUnlocked(unit)) return false
        if (kind.ordinal == 0) return true
        val lvl = Content.level(unit.levelId)
        if (lvl.index < levelIndex()) return true
        return starsOf(unit.id, LessonKind.entries[kind.ordinal - 1]) > 0
    }

    // ---------- المراجعة الذكية ----------

    private val intervals = longArrayOf(0, 1, 3, 7, 16)

    /** يُسجل نتيجة كلمة: الصحيح ينقلها لصندوق أعلى (مراجعة أبعد)، والخطأ يعيدها للبداية */
    fun recordWord(key: String, correct: Boolean) {
        val cur = srs[key]
        val box = when {
            cur == null -> if (correct) 2 else 1
            correct -> (cur.box + 1).coerceAtMost(5)
            else -> 1
        }
        srs[key] = SrsItem(box, today() + intervals[box - 1])
        writeSrs()
    }

    /** يضيف كلمات الدرس للمراجعة بعد تعلّمها */
    fun learnWords(words: List<Word>) {
        var changed = false
        words.forEach { w ->
            if (!srs.containsKey(w.key)) { srs[w.key] = SrsItem(1, today() + 1); changed = true }
        }
        if (changed) writeSrs()
    }

    fun dueKeys(): List<String> {
        val d = today()
        return srs.entries.filter { it.value.dueDay <= d }.sortedBy { it.value.box }.map { it.key }
    }

    fun weakKeys(limit: Int): List<String> =
        srs.entries.sortedWith(compareBy({ it.value.box }, { it.value.dueDay })).take(limit).map { it.key }

    fun addReviewed(n: Int) {
        reviewedCount += n
        sp.edit().putInt("reviewedCount", reviewedCount).apply()
        checkAchievements()
    }

    // ---------- الألعاب ----------

    fun gameFinished(gameId: String, score: Int): Boolean {
        gamesPlayed += 1
        val best = bestScores[gameId] ?: 0
        val record = score > best
        if (record) bestScores[gameId] = score
        sp.edit().putInt("gamesPlayed", gamesPlayed).apply()
        writeIntMap("bestScores", bestScores)
        checkAchievements()
        return record
    }

    // ---------- الأوسمة ----------

    /** آخر وسام حصل عليه المستخدم ليُعرض له احتفال */
    var newAchievement by mutableStateOf<String?>(null)

    fun unlock(id: String) {
        if (achievements.containsKey(id)) return
        achievements[id] = System.currentTimeMillis()
        val o = JSONObject()
        achievements.forEach { (k, v) -> o.put(k, v) }
        sp.edit().putString("achievements", o.toString()).apply()
        if (Achievements.all.any { it.id == id }) newAchievement = id
    }

    private fun checkAchievements() {
        if (streak >= 3) unlock("streak_3")
        if (streak >= 7) unlock("streak_7")
        if (streak >= 30) unlock("streak_30")
        if (xpTotal >= 500) unlock("xp_500")
        if (xpTotal >= 2000) unlock("xp_2000")
        if (xpTotal >= 10000) unlock("xp_10000")
        if (perfectCount >= 5) unlock("perfect_5")
        if (gamesPlayed >= 10) unlock("games_10")
        if (reviewedCount >= 50) unlock("review_50")
        if (srs.size >= 100) unlock("words_100")
    }

    // ---------- الحفظ ----------

    private fun readJson(key: String): JSONObject =
        runCatching { JSONObject(sp.getString(key, "{}") ?: "{}") }.getOrElse { JSONObject() }

    private fun readIntMap(key: String): Map<String, Int> {
        val o = readJson(key)
        val m = HashMap<String, Int>()
        o.keys().forEach { m[it] = o.optInt(it) }
        return m
    }

    private fun writeIntMap(key: String, map: Map<String, Int>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        sp.edit().putString(key, o.toString()).apply()
    }

    private fun writeSrs() {
        val o = JSONObject()
        srs.forEach { (k, v) -> o.put(k, "${v.box},${v.dueDay}") }
        sp.edit().putString("srs", o.toString()).apply()
    }
}

data class Achievement(val id: String, val emoji: String, val ar: String, val fr: String, val en: String)

object Achievements {
    val all = listOf(
        Achievement("placement", "🧭", "عرفت مستواك", "Niveau trouvé", "Level found"),
        Achievement("first_lesson", "🌟", "أول درس", "Première leçon", "First lesson"),
        Achievement("streak_3", "🔥", "3 أيام متتالية", "3 jours de suite", "3-day streak"),
        Achievement("streak_7", "⚡", "أسبوع كامل", "Une semaine complète", "Full week"),
        Achievement("streak_30", "👑", "شهر بلا انقطاع", "Un mois sans pause", "30-day streak"),
        Achievement("xp_500", "🥉", "500 نقطة", "500 points", "500 points"),
        Achievement("xp_2000", "🥈", "2000 نقطة", "2000 points", "2000 points"),
        Achievement("xp_10000", "🥇", "10000 نقطة", "10000 points", "10000 points"),
        Achievement("perfect_5", "💯", "5 دروس بلا خطأ", "5 leçons parfaites", "5 perfect lessons"),
        Achievement("games_10", "🎮", "10 ألعاب", "10 jeux", "10 games"),
        Achievement("review_50", "🔁", "50 مراجعة", "50 révisions", "50 reviews"),
        Achievement("words_100", "📚", "100 كلمة", "100 mots", "100 words"),
        Achievement("level_up", "🚀", "تجاوزت مستوى", "Niveau réussi", "Level passed"),
    )
}
