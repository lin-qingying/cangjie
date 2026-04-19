package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

class CfirNameConflictsTrackerImpl : CfirNameConflictsTracker() {
    private data class ClassifierRedeclarationImpl(
        override val classifierSymbol: CfirClassLikeSymbol<*>,
        override val containingFile: CfirFile?,
    ) : ClassifierRedeclaration()

    private val redeclaredClassifiers: MutableMap<ClassId, Set<ClassifierRedeclarationImpl>> = hashMapOf()

    override fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration> =
        redeclaredClassifiers[classId].orEmpty()

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    ) {
        redeclaredClassifiers.merge(
            classId,
            linkedSetOf(
                ClassifierRedeclarationImpl(newSymbol, newSymbolFile),
                ClassifierRedeclarationImpl(prevSymbol, prevSymbolFile),
            ),
        ) { first, second -> first + second }
    }
}


