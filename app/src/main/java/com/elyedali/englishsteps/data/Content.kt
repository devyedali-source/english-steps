package com.elyedali.englishsteps.data

import android.content.Context
import com.elyedali.englishsteps.i18n.I18n
import org.json.JSONArray
import org.json.JSONObject

/** نص بثلاث لغات، يُعرض حسب لغة الواجهة */
data class Txt(val ar: String, val fr: String, val en: String) {
    fun get(): String = when (I18n.lang) {
        "fr" -> fr
        "en" -> en
        else -> ar
    }
}

data class Word(
    val unitId: String,
    val en: String,
    val pic: String,
    val ar: String,
    val fr: String,
) {
    val key: String get() = "$unitId|$en"

    /** معنى الكلمة بلغة الواجهة (في الواجهة الإنجليزية يبقى الرسم هو المعنى) */
    fun meaning(): String = when (I18n.lang) {
        "fr" -> fr
        "en" -> en
        else -> ar
    }
}

data class Example(val en: String, val ar: String, val fr: String) {
    fun translation(): String? = when (I18n.lang) {
        "fr" -> fr
        "en" -> null
        else -> ar
    }
}

data class ChoiceItem(val q: String, val options: List<String>, val answer: Int)

data class Grammar(
    val title: Txt,
    val explain: Txt,
    val examples: List<Example>,
    val choice: List<ChoiceItem>,
    val order: List<String>,
)

data class Line(val who: String, val en: String, val ar: String, val fr: String) {
    fun translation(): String? = when (I18n.lang) {
        "fr" -> fr
        "en" -> null
        else -> ar
    }
}

data class Dialogue(
    val title: Txt,
    val scene: String,
    val lines: List<Line>,
    val questions: List<ChoiceItem>,
)

data class LessonUnit(
    val id: String,
    val levelId: String,
    val index: Int,
    val emoji: String,
    val title: Txt,
    val vocab: List<Word>,
    val grammar: Grammar,
    val dialogue: Dialogue,
)

data class Level(
    val id: String,
    val index: Int,
    val emoji: String,
    val title: Txt,
    val units: List<LessonUnit>,
)

/** أنواع الدروس داخل كل وحدة، بالترتيب */
enum class LessonKind(val id: String, val emoji: String) {
    VOCAB("vocab", "🖼️"),
    GRAMMAR("grammar", "🧩"),
    DIALOGUE("dialogue", "💬"),
    PRACTICE("practice", "🏆");

    companion object {
        fun of(id: String): LessonKind = entries.firstOrNull { it.id == id } ?: VOCAB
    }
}

/** محتوى التطبيق كله مدمج داخل التطبيق (assets/content.json) ويعمل دون إنترنت */
object Content {
    var levels: List<Level> = emptyList()
        private set

    /** عدد الوحدات المجانية في بداية كل مستوى */
    const val FREE_UNITS = 2

    fun load(context: Context) {
        if (levels.isNotEmpty()) return
        val text = context.assets.open("content.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("levels")
        levels = (0 until arr.length()).map { li -> parseLevel(arr.getJSONObject(li), li) }
    }

    fun level(id: String): Level = levels.firstOrNull { it.id == id } ?: levels.first()
    fun unit(id: String): LessonUnit? = levels.asSequence().flatMap { it.units.asSequence() }.firstOrNull { it.id == id }
    fun allWords(): List<Word> = levels.flatMap { l -> l.units.flatMap { it.vocab } }
    fun wordByKey(key: String): Word? {
        val unitId = key.substringBefore('|')
        return unit(unitId)?.vocab?.firstOrNull { it.key == key }
    }

    private fun txt(o: JSONObject) = Txt(o.optString("ar"), o.optString("fr"), o.optString("en"))

    private fun strings(a: JSONArray) = (0 until a.length()).map { a.getString(it) }

    private fun choices(a: JSONArray) = (0 until a.length()).map {
        val o = a.getJSONObject(it)
        ChoiceItem(o.getString("q"), strings(o.getJSONArray("options")), o.getInt("answer"))
    }

    private fun parseLevel(o: JSONObject, index: Int): Level {
        val id = o.getString("id")
        val ua = o.getJSONArray("units")
        val units = (0 until ua.length()).map { ui ->
            val u = ua.getJSONObject(ui)
            val uid = u.getString("id")
            val va = u.getJSONArray("vocab")
            val vocab = (0 until va.length()).map {
                val w = va.getJSONObject(it)
                Word(uid, w.getString("en"), w.getString("pic"), w.getString("ar"), w.getString("fr"))
            }
            val g = u.getJSONObject("grammar")
            val ea = g.getJSONArray("examples")
            val grammar = Grammar(
                title = txt(g.getJSONObject("title")),
                explain = txt(g.getJSONObject("explain")),
                examples = (0 until ea.length()).map {
                    val e = ea.getJSONObject(it)
                    Example(e.getString("en"), e.getString("ar"), e.getString("fr"))
                },
                choice = choices(g.getJSONArray("choice")),
                order = strings(g.getJSONArray("order")),
            )
            val d = u.getJSONObject("dialogue")
            val la = d.getJSONArray("lines")
            val dialogue = Dialogue(
                title = txt(d.getJSONObject("title")),
                scene = d.getString("scene"),
                lines = (0 until la.length()).map {
                    val l = la.getJSONObject(it)
                    Line(l.getString("who"), l.getString("en"), l.getString("ar"), l.getString("fr"))
                },
                questions = choices(d.getJSONArray("questions")),
            )
            LessonUnit(uid, id, ui, u.getString("emoji"), txt(u.getJSONObject("title")), vocab, grammar, dialogue)
        }
        return Level(id, index, o.getString("emoji"), txt(o.getJSONObject("title")), units)
    }
}
