package org.cangnova.cangjie.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * Analysis API 的公开符号抽象。
 *
 * 符号是 Analysis API 暴露语义信息的核心载体，后续所有类型、作用域、引用解析和渲染能力
 * 都围绕它展开。
 */
interface CaSymbol : CaLifetimeOwner {
    val containingModule: CaModule

    val name: String?
}

interface CaPackageSymbol : CaSymbol {
    val fqName: FqName

    override val name: String
        get() = fqName.shortName().asString()
}

interface CaFileSymbol : CaSymbol {
    val file: CjFile

    val packageFqName: FqName

    override val name: String
        get() = file.name
}

interface CaDeclarationSymbol : CaSymbol {
    val psi: PsiElement?
}

interface CaClassLikeSymbol : CaDeclarationSymbol {
    val classId: ClassId

    override val name: String
        get() = classId.shortClassName.asString()
}

interface CaCallableSymbol : CaDeclarationSymbol {
    val callableId: CallableId?

    override val name: String?
        get() = callableId?.callableName?.asString()
}
