

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * An [LLMultiClassLikeSymbolProvider] is able to provide all class-like symbols for a single [ClassId].
 *
 * Its behavior is in contrast to [getClassLikeSymbolByClassId][org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider.getClassLikeSymbolByClassId],
 * which only returns the first class-like symbol with the class ID, whereas [getAllClassLikeSymbolsByClassId] returns *all* such symbols.
 */
internal interface LLMultiClassLikeSymbolProvider : LLPsiAwareSymbolProvider {
    /**
     * Returns all [CfirClassLikeSymbol]s with the given [classId].
     */
    fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>>
}
