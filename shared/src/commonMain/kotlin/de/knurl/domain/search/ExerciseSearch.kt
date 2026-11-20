package de.knurl.domain.search

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.ExerciseLevel
import de.knurl.domain.model.Muscle
import kotlin.math.min

data class ExerciseFilter(
    val muscle: Muscle? = null,
    val equipment: Equipment? = null,
    val level: ExerciseLevel? = null,
    /** Only the person's own exercises. */
    val customOnly: Boolean = false,
) {
    /** Archived own exercises never show up in the library or the search. */
    fun matches(e: Exercise) =
        !e.archived && (!customOnly || e.isCustom) &&
            (muscle == null || e.primary == muscle) && (equipment == null || e.equipment == equipment) && (level == null || e.level == level)
}

/**
 * Finds exercises the way people type in a gym: either language, umlauts or not ("bankdrucken",
 * "bankdrücken", "bankdruecken"), gym shorthand ("BD", "RDL", "OHP"), muscle names ("bizeps"),
 * and one typo per word. Every word of the query has to match something.
 */
object ExerciseSearch {

    /** Shorthand → words to look for instead (already normalised). */
    private val synonyms: Map<String, List<String>> = mapOf(
        "bd" to listOf("bankdrucken", "bench"), "kh" to listOf("kurzhantel", "dumbbell"), "lh" to listOf("langhantel", "barbell"),
        "db" to listOf("dumbbell", "kurzhantel"), "bb" to listOf("barbell", "langhantel"), "kb" to listOf("kettlebell"),
        "ohp" to listOf("overhead", "military", "schulterdrucken"), "rdl" to listOf("romanian", "rumanisches"),
        "dl" to listOf("deadlift", "kreuzheben"), "sz" to listOf("sz", "ez"), "ez" to listOf("ez", "sz"),
        "squat" to listOf("squat", "kniebeuge"), "kniebeuge" to listOf("kniebeuge", "squat"),
        "klimmzug" to listOf("klimmzug", "pullups", "pull"), "klimmzuge" to listOf("klimmzug", "pullups"),
        "latzug" to listOf("latzug", "pulldown"), "bench" to listOf("bench", "bankdrucken"),
        "rudern" to listOf("rudern", "row"), "row" to listOf("row", "rudern"), "dips" to listOf("dips"),
        "butterfly" to listOf("butterfly", "fliegende", "fly"), "fliegende" to listOf("fliegende", "fly", "butterfly"),
    )

    /** Muscle words in both languages, so "bizeps" or "brust" finds the exercises for it. */
    private val muscleWords: Map<Muscle, List<String>> = mapOf(
        Muscle.CHEST to listOf("brust", "chest", "pecs"), Muscle.BACK to listOf("rucken", "back", "lat", "lats"),
        Muscle.SHOULDERS to listOf("schulter", "schultern", "shoulders", "delts"), Muscle.BICEPS to listOf("bizeps", "biceps"),
        Muscle.TRICEPS to listOf("trizeps", "triceps"), Muscle.FOREARMS to listOf("unterarm", "unterarme", "forearms"),
        Muscle.QUADS to listOf("quadrizeps", "oberschenkel", "quads", "beine", "legs"),
        Muscle.HAMSTRINGS to listOf("beinbizeps", "hamstrings", "beine", "legs"), Muscle.GLUTES to listOf("gesass", "po", "glutes"),
        Muscle.CALVES to listOf("waden", "calves"), Muscle.CORE to listOf("bauch", "rumpf", "core", "abs"),
    )

    fun search(
        exercises: List<Exercise>,
        query: String,
        filter: ExerciseFilter = ExerciseFilter(),
        german: Boolean = true,
    ): List<Exercise> {
        val candidates = exercises.filter(filter::matches)
        val words = tokens(query)
        if (words.isEmpty()) return candidates.sortedBy { normalize(it.name(german)) }
        return candidates
            .mapNotNull { e -> score(e, words, german)?.let { e to it } }
            .sortedWith(compareByDescending<Pair<Exercise, Int>> { it.second }.thenBy { normalize(it.first.name(german)) })
            .map { it.first }
    }

    /** Null when some query word matches nothing; otherwise higher is better. */
    private fun score(e: Exercise, words: List<String>, german: Boolean): Int? {
        val shown = tokens(e.name(german))
        val other = tokens(e.name(!german))
        val muscles = muscleWords[e.primary].orEmpty()
        var total = 0
        for (word in words) {
            val alternatives = listOf(word) + synonyms[word].orEmpty()
            val best = alternatives.maxOf { w ->
                maxOf(match(w, shown) * 3, match(w, other) * 2, if (muscles.any { it.startsWith(w) }) 3 else 0)
            }
            if (best == 0) return null
            total += best
        }
        // A name that starts with the query comes first; among equals, the shorter (plainer) name wins.
        val phrase = words.joinToString(" ")
        if (normalize(e.name(german)).startsWith(phrase)) total += 20 else if (normalize(e.name(!german)).startsWith(phrase)) total += 15
        return total * 10 - shown.size
    }

    /** 4 = word starts a name word, 3 = inside a name word, 1 = one typo away. */
    private fun match(word: String, nameWords: List<String>): Int {
        var best = 0
        for (token in nameWords) {
            best = maxOf(best, when {
                token.startsWith(word) -> 4
                word.length >= 3 && token.contains(word) -> 3
                word.length >= 4 && closeEnough(word, token) -> 1
                else -> 0
            })
        }
        return best
    }

    /** One edit for words of 4–7 letters, two from 8; compared with the same-length start of the name word. */
    private fun closeEnough(word: String, token: String): Boolean {
        val allowed = if (word.length >= 8) 2 else 1
        val prefix = token.take(word.length + allowed)
        return distance(word, token.take(word.length)) <= allowed || distance(word, prefix) <= allowed
    }

    /** Damerau-Levenshtein (optimal string alignment). */
    internal fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
        }
        return d[a.length][b.length]
    }

    fun tokens(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    /** Lower case, accents off, ä/ae → a (both spellings meet), everything else becomes a space. */
    fun normalize(text: String): String {
        val out = StringBuilder()
        for (c in text.lowercase()) {
            out.append(
                when (c) {
                    'ä', 'à', 'á', 'â' -> "a"
                    'ö', 'ò', 'ó', 'ô' -> "o"
                    'ü', 'ù', 'ú', 'û' -> "u"
                    'é', 'è', 'ê' -> "e"
                    'ß' -> "ss"
                    in 'a'..'z', in '0'..'9' -> c.toString()
                    else -> " "
                },
            )
        }
        return out.toString().replace("ae", "a").replace("oe", "o").replace("ue", "u").replace(Regex(" +"), " ").trim()
    }
}
