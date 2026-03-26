package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

enum class CfirExtendSemanticOrigin {
    SOURCE,
    LIBRARY,
    SYNTHETIC,
    OTHER,
}

data class CfirExtendSemanticModel(
    val declaration: CfirExtend,
    val packageFqName: FqName,
    val fileName: String,
    val declarationIndexInFile: Int,
    val targetClassId: ClassId?,
    val targetClassKind: CfirClassKind?,
    val inheritedInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    val inheritedInterfaceClassIds: List<ClassId>,
    val inheritedInterfaceSemanticKeys: List<String>,
    val origin: CfirExtendSemanticOrigin,
)

internal fun CfirDeclarationOrigin.toExtendSemanticOrigin(): CfirExtendSemanticOrigin = when (this) {
    CfirDeclarationOrigin.Source -> CfirExtendSemanticOrigin.SOURCE
    CfirDeclarationOrigin.Library -> CfirExtendSemanticOrigin.LIBRARY
    CfirDeclarationOrigin.Synthetic.Default,
    CfirDeclarationOrigin.ImplicitDefault,
    CfirDeclarationOrigin.Extension -> CfirExtendSemanticOrigin.SYNTHETIC
    else -> CfirExtendSemanticOrigin.OTHER
}

