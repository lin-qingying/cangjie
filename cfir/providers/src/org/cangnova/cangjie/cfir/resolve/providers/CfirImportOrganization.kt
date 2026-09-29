package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/** 按 CJO 规范包身份或源码容器的组织声明验证导入候选，禁止借用无组织同名声明。 */
fun CfirBasedSymbol<*>.belongsToImportOrganization(organizationName: Name?, packageFqName: FqName): Boolean {
    if (organizationName == null) return true
    if (packageFqName.asString().endsWith("@${organizationName.asString()}")) return true
    val provider = cfir.moduleData.session.cfirProvider
    val file = when (this) {
        is CfirClassLikeSymbol<*> -> provider.getCfirClassifierContainerFileIfAny(this)
        is CfirCallableSymbol<*> -> provider.getCfirCallableContainerFile(this)
        else -> null
    }
    return file?.packageDirective?.organizationName == organizationName
}
