package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Source declaration provider aligned with Kotlin FIR provider abstractions.
 */
abstract class CfirProvider : CfirSessionComponent {
    abstract val symbolProvider: CfirSymbolProvider

    abstract fun getCfirFilesByPackage(fqName: FqName): List<CfirFile>

    abstract fun getClassByClassId(classId: ClassId): CfirClass?

    abstract fun getClassNamesInPackage(fqName: FqName): Set<Name>
}
