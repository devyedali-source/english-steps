package com.elyedali.englishsteps.engine

import com.elyedali.englishsteps.data.ChoiceItem
import com.elyedali.englishsteps.data.Content
import com.elyedali.englishsteps.data.LessonKind
import com.elyedali.englishsteps.data.LessonUnit
import com.elyedali.englishsteps.data.Level
import com.elyedali.englishsteps.data.Progress
import com.elyedali.englishsteps.data.Word
import com.elyedali.englishsteps.i18n.I18n
import com.elyedali.englishsteps.i18n.t

/** سؤال واحد في درس أو اختبار */
sealed class Question {
    /** المفتاح الذي يُسجل في المراجعة الذكية (للكلمات فقط) */
    abstract val wordKey: String?

    data class Choice(
        val prompt: String,
        val sentence: String? = null,
        val pic: String? = null,
        val speak: String? = null,
        val autoSpeak: Boolean = false,
        val options: List<String>,
        val answer: Int,
        /** الخيارات رسوم كبيرة بدل نصوص */
        val optionPics: Boolean = false,
        /** الخيارات نصوص إنجليزية تُنطق عند الضغط */
        val optionsEnglish: Boolean = false,
        override val wordKey: String? = null,
    ) : Question()

    /** ترتيب كلمات لتكوين جملة */
    data class Order(
        val prompt: String,
        val answer: List<String>,
        val tiles: List<String>,
        override val wordKey: String? = null,
    ) : Question()

    /** تهجئة كلمة بالحروف */
    data class Spell(
        val pic: String,
        val meaning: String,
        val word: String,
        val tiles: List<String>,
        override val wordKey: String? = null,
    ) : Question()
}

object Questions {

    // ---------- أسئلة الكلمات ----------

    private fun distractors(w: Word, pool: List<Word>, n: Int = 3, key: (Word) -> String): List<Word> {
        val seen = HashSet<String>().apply { add(key(w)); add(w.en.lowercase()) }
        val seenPics = HashSet<String>().apply { add(w.pic) }
        val out = ArrayList<Word>()
        val sources = listOf(
            pool.filter { it.unitId == w.unitId },
            pool.filter { it.unitId != w.unitId },
            Content.allWords(),
        )
        for (src in sources) {
            for (c in src.shuffled()) {
                if (out.size >= n) break
                if (key(c) in seen || c.en.lowercase() in seen || c.pic in seenPics) continue
                seen.add(key(c)); seen.add(c.en.lowercase()); seenPics.add(c.pic)
                out.add(c)
            }
        }
        return out
    }

    private fun <T> withAnswer(answer: T, others: List<T>): Pair<List<T>, Int> {
        val all = (others + answer).shuffled()
        return all to all.indexOf(answer)
    }

    /** يرى الرسم ويختار الكلمة الإنجليزية */
    fun picToWord(w: Word, pool: List<Word>): Question {
        val (opts, ans) = withAnswer(w, distractors(w, pool) { it.en })
        return Question.Choice(
            prompt = t("اختر الكلمة المناسبة للرسم", "Choisis le mot qui correspond au dessin", "Choose the word for the picture"),
            pic = w.pic, options = opts.map { it.en }, answer = ans, optionsEnglish = true, wordKey = w.key,
        )
    }

    /** يرى الكلمة الإنجليزية ويختار معناها (أو رسمها في الواجهة الإنجليزية) */
    fun wordToMeaning(w: Word, pool: List<Word>): Question {
        if (I18n.lang == "en") return wordToPic(w, pool)
        val (opts, ans) = withAnswer(w, distractors(w, pool) { it.meaning() })
        return Question.Choice(
            prompt = t("ما معنى هذه الكلمة؟", "Que signifie ce mot ?", "What does this word mean?"),
            sentence = w.en, speak = w.en, options = opts.map { it.meaning() }, answer = ans, wordKey = w.key,
        )
    }

    fun wordToPic(w: Word, pool: List<Word>): Question {
        val (opts, ans) = withAnswer(w, distractors(w, pool) { it.pic })
        return Question.Choice(
            prompt = t("اختر الرسم المناسب", "Choisis le bon dessin", "Choose the right picture"),
            sentence = w.en, speak = w.en, options = opts.map { it.pic }, answer = ans, optionPics = true, wordKey = w.key,
        )
    }

    /** يسمع الكلمة ويختار رسمها */
    fun listenToPic(w: Word, pool: List<Word>): Question {
        val (opts, ans) = withAnswer(w, distractors(w, pool) { it.pic })
        return Question.Choice(
            prompt = t("استمع واختر الرسم", "Écoute et choisis le dessin", "Listen and choose the picture"),
            speak = w.en, autoSpeak = true, options = opts.map { it.pic }, answer = ans, optionPics = true, wordKey = w.key,
        )
    }

    /** يرى المعنى ويختار الكلمة الإنجليزية */
    fun meaningToWord(w: Word, pool: List<Word>): Question {
        if (I18n.lang == "en") return picToWord(w, pool)
        val (opts, ans) = withAnswer(w, distractors(w, pool) { it.en })
        return Question.Choice(
            prompt = t("كيف نقول بالإنجليزية؟", "Comment dit-on en anglais ?", "How do you say it in English?"),
            sentence = w.meaning(), pic = w.pic, options = opts.map { it.en }, answer = ans, optionsEnglish = true, wordKey = w.key,
        )
    }

    fun canSpell(w: Word): Boolean = w.en.length in 3..9 && w.en.all { it.isLetter() }

    fun spell(w: Word): Question {
        val letters = w.en.lowercase().map { it.toString() }
        val extra = "aeioustrnl".map { it.toString() }.shuffled().take(2)
        return Question.Spell(
            pic = w.pic, meaning = if (I18n.lang == "en") "" else w.meaning(), word = w.en.lowercase(),
            tiles = (letters + extra).shuffled(), wordKey = w.key,
        )
    }

    /** سؤال كلمة بنوع متغير حتى لا يمل المتعلم */
    fun wordQuestion(w: Word, pool: List<Word>, variant: Int): Question = when (variant % 5) {
        0 -> picToWord(w, pool)
        1 -> wordToMeaning(w, pool)
        2 -> listenToPic(w, pool)
        3 -> if (canSpell(w)) spell(w) else meaningToWord(w, pool)
        else -> meaningToWord(w, pool)
    }

    // ---------- أسئلة القواعد والحوار ----------

    fun choice(c: ChoiceItem, prompt: String): Question {
        val (opts, ans) = withAnswer(c.options[c.answer], c.options.filterIndexed { i, _ -> i != c.answer })
        return Question.Choice(prompt = prompt, sentence = c.q, options = opts, answer = ans, optionsEnglish = true)
    }

    fun grammarChoice(c: ChoiceItem) = choice(c, t("اختر الكلمة الصحيحة للفراغ", "Choisis le bon mot pour compléter", "Choose the right word for the gap"))

    fun comprehension(c: ChoiceItem) = choice(c, t("أجب حسب الحوار", "Réponds selon le dialogue", "Answer from the dialogue"))

    fun order(sentence: String): Question {
        val words = sentence.trim().trimEnd('.', '?', '!').split(" ").filter { it.isNotBlank() }
        var tiles = words.shuffled()
        var guard = 0
        while (tiles == words && words.size > 1 && guard++ < 10) tiles = words.shuffled()
        return Question.Order(
            prompt = t("رتّب الكلمات لتكوين جملة صحيحة", "Remets les mots dans l'ordre", "Put the words in order"),
            answer = words, tiles = tiles,
        )
    }

    private fun shortLines(u: LessonUnit): List<String> =
        u.dialogue.lines.map { it.en.trim() }
            .filter { s -> s.count { it == ' ' } in 2..7 && s.count { it == '.' || it == '?' || it == '!' } <= 1 && !s.contains(',') }

    // ---------- بناء الدروس ----------

    fun lesson(u: LessonUnit, kind: LessonKind): List<Question> {
        val pool = Content.level(u.levelId).units.flatMap { it.vocab }
        return when (kind) {
            LessonKind.VOCAB -> u.vocab.mapIndexed { i, w -> wordQuestion(w, pool, i) }.shuffled()
            LessonKind.GRAMMAR -> (u.grammar.choice.map { grammarChoice(it) } + u.grammar.order.map { order(it) }).shuffled()
            LessonKind.DIALOGUE -> u.dialogue.questions.map { comprehension(it) } +
                shortLines(u).shuffled().take(2).map { order(it) }
            LessonKind.PRACTICE -> {
                val words = u.vocab.shuffled().take(6).mapIndexed { i, w -> wordQuestion(w, pool, i + 1) }
                val gram = u.grammar.choice.shuffled().take(3).map { grammarChoice(it) }
                val simpleExamples = u.grammar.examples.map { it.en.trim() }.filter { s ->
                    !s.contains(',') && s.dropLast(1).none { it == '.' || it == '?' || it == '!' } && s.count { it == ' ' } <= 8
                }
                val ord = (u.grammar.order + simpleExamples).shuffled().take(2).map { order(it) }
                val dia = u.dialogue.questions.shuffled().take(1).map { comprehension(it) }
                (words + gram + ord + dia).shuffled()
            }
        }
    }

    /** اختبار تجاوز المستوى: 20 سؤالًا من كل وحدات المستوى */
    fun levelExam(level: Level): List<Question> {
        val pool = level.units.flatMap { it.vocab }
        val words = pool.shuffled().take(8).mapIndexed { i, w -> wordQuestion(w, pool, i) }
        val gram = level.units.flatMap { it.grammar.choice }.shuffled().take(7).map { grammarChoice(it) }
        val ord = level.units.flatMap { it.grammar.order }.shuffled().take(2).map { order(it) }
        val dia = level.units.flatMap { it.dialogue.questions }.shuffled().take(3).map { comprehension(it) }
        return (words + gram + ord + dia).shuffled()
    }

    /** أسئلة مرحلة واحدة من امتحان تحديد المستوى */
    fun placementStage(level: Level): List<Question> {
        val pool = level.units.flatMap { it.vocab }
        val gram = level.units.flatMap { it.grammar.choice }.shuffled().take(4).map { grammarChoice(it) }
        val words = pool.shuffled().take(2).mapIndexed { i, w -> if (i == 0) picToWord(w, pool) else meaningToWord(w, pool) }
        return (gram + words).shuffled()
    }

    /** الكلمات التي تعلّمها المستخدم (للألعاب والمراجعة) */
    fun learnedWords(): List<Word> {
        val learned = Progress.srs.keys.mapNotNull { Content.wordByKey(it) }
        if (learned.size >= 6) return learned
        val lvl = Content.level(Progress.currentLevel)
        return (learned + lvl.units.first().vocab + lvl.units.getOrNull(1)?.vocab.orEmpty()).distinctBy { it.key }
    }

    /** المراجعة الذكية: الكلمات المستحقة اليوم أولًا، ثم الأضعف */
    fun review(limit: Int = 12): List<Question> {
        val keys = (Progress.dueKeys() + Progress.weakKeys(limit)).distinct().take(limit)
        val words = keys.mapNotNull { Content.wordByKey(it) }.ifEmpty { learnedWords().shuffled().take(limit) }
        val pool = learnedWords()
        return words.mapIndexed { i, w -> wordQuestion(w, pool, i) }
    }

    /** تحدي اليوم: خليط من كل ما تعلّمه */
    fun dailyChallenge(): List<Question> {
        val pool = learnedWords()
        val words = pool.shuffled().take(6).mapIndexed { i, w -> wordQuestion(w, pool, i + 2) }
        val units = pool.map { it.unitId }.distinct().mapNotNull { Content.unit(it) }
        val gram = units.flatMap { it.grammar.choice }.shuffled().take(3).map { grammarChoice(it) }
        val ord = units.flatMap { it.grammar.order }.shuffled().take(1).map { order(it) }
        return (words + gram + ord).shuffled()
    }
}
