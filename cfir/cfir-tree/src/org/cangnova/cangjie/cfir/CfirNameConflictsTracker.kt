package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

abstract class CfirNameConflictsTracker : CfirSessionComponent {
    abstract class ClassifierRedeclaration {
        abstract val classifierSymbol: CfirClassLikeSymbol<*>
        abstract val containingFile: CfirFile?
    }

    abstract fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration>

    abstract fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    )
}

val CfirSession.nameConflictsTracker: CfirNameConflictsTracker? by CfirSession.nullableSessionComponentAccessor()
