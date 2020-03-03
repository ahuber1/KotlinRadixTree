package com.ahuber.collections

/**
 * The result of searching for a word in a radix or prefix tree.
 */
sealed class WordSearchResult  {

    /**
     * The search string.
     */
    abstract val searchString: String

    /**
     * Indicates that no word matching the input string, or a word that shares a common prefix with the search string
     * exists in the set.
     */
    data class NoMatch(override val searchString: String): WordSearchResult()

    /**
     * Indicates that an exact match for the input string was found in the set.
     */
    data class ExactMatch(
            override val searchString: String,

            /**
             * A list of complete words in the set that start with [searchString] but are longer than [searchString].
             */
            val longerWords: List<String>
    ): WordSearchResult()

    /**
     * Indicates that a word that shares a common prefix with the search string exists in the set.
     */
    data class PartialMatch(

            override val searchString: String,

            /**
             * A list of complete words in the set that start with [searchString].
             */
            val possibleMatches: List<String>
    ): WordSearchResult()
}

/**
 * Unpacks the list of *complete* words that are inside this [WordSearchResult].
 * @return
 * - If this is [WordSearchResult.NoMatch], then an empty list is returned.
 * - If this is [WordSearchResult.ExactMatch], then a list containing the original search string and
 *   [WordSearchResult.ExactMatch.longerWords] is returned.
 * - If this is [WordSearchResult.PartialMatch], then [WordSearchResult.PartialMatch.possibleMatches] is returned.
 */
fun WordSearchResult.unpack(): List<String> = when(this) {
    is WordSearchResult.NoMatch -> emptyList()
    is WordSearchResult.ExactMatch -> listOf(this.searchString) + this.longerWords
    is WordSearchResult.PartialMatch -> this.possibleMatches
}