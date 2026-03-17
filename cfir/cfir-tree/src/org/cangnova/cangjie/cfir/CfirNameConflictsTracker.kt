package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.name.ClassId

abstract class CfirNameConflictsTracker : CfirSessionComponent {
    abstract class ClassifierRedeclaration {
        abstract val classifierSymbol: CfirClassifierSymbol<*>
        abstract val containingFile: CfirFile?
    }

    abstract fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration>

    abstract fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassifierSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassifierSymbol<*>,
        prevSymbolFile: CfirFile?,
    )
}

val CfirSession.nameConflictsTracker: CfirNameConflictsTracker? by CfirSession.nullableSessionComponentAccessor()

