

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.structure

/**
 * [LLSessionStatistics] aggregates statistics about a specific [LLCfirSession][org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession]
 * in the session structure graph.
 *
 * The **weight** of a session is the weight of all CFIR elements that it contains. This weight is approximated by the shallow sizes of CFIR
 * elements and disregards secondary objects like CFIR symbols, source elements (PSI), names, and so on. The weight is thus not an absolute
 * number meant to be read as "memory consumption of the session," but rather a measure of the relative weight between sessions, or the same
 * session over time (comparing multiple session structure snapshots).
 *
 * @property cangjieWeight The weight of all仓颉 CFIR elements in the session.
 * @property javaWeight The weight of all *Java* CFIR elements in the session.
 * @property lifetime The time in seconds since the creation of the session.
 */
internal class LLSessionStatistics(
    val cangjieWeight: Long,
    val javaWeight: Long,
    val lifetime: Double,
) {
    /**
     * The total weight of the session, combining both [cangjieWeight] and [javaWeight].
     */
    val weight: Long get() = cangjieWeight + javaWeight

    companion object {
        val ZERO = LLSessionStatistics(0, 0, 0.0)
    }
}
