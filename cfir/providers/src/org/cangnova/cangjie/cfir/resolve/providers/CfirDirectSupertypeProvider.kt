package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId

interface CfirDirectSupertypeProvider : CfirSessionComponent {
    fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef>
}
