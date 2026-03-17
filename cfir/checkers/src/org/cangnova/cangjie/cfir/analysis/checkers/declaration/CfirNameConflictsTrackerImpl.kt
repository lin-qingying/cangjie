package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.name.ClassId

class CfirNameConflictsTrackerImpl : CfirNameConflictsTracker() {
    private data class ClassifierRedeclarationImpl(
        override val classifierSymbol: CfirClassifierSymbol<*>,
        override val containingFile: CfirFile?,
    ) : ClassifierRedeclaration()

    private val redeclaredClassifiers: MutableMap<ClassId, Set<ClassifierRedeclarationImpl>> = hashMapOf()

    override fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration> =
        redeclaredClassifiers[classId].orEmpty()

    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassifierSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassifierSymbol<*>,
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


