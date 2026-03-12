package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.cfirProvider
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

internal class CfirTypeRefResolver(
    private val session: CfirSession,
) {
    fun resolveClass(typeRef: CfirTypeRef): CfirClass? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        val classId = ClassId(packageFqName, className)
        return session.cfirProvider.getClassByClassId(classId)
    }
}
