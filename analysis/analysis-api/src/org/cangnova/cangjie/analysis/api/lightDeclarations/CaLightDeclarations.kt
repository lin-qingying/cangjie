package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 仓颉声明视图的公开种类。
 *
 * 这里的 `light` 仅表示“只读、轻量的声明投影”，
 * 不引入 Java PSI 语义，也不承诺与某个后端产物一一对应。
 */
enum class CaLightDeclarationKind {
    PACKAGE,
    CLASS_LIKE,
    EXTEND,
    CALLABLE,
}

/**
 * 声明视图的来源种类。
 */
enum class CaLightDeclarationOriginKind {
    SOURCE_PSI,
    DECOMPILED_PSI,
    SYNTHETIC,
}

/**
 * 声明视图的来源信息。
 *
 * `sourceElement` 与 `containingFile` 仅在当前来源可稳定恢复时暴露；
 * 对纯合成成员，仅保留 `description` 作为调试与导出的稳定说明。
 */
data class CaLightDeclarationOrigin(
    val kind: CaLightDeclarationOriginKind,
    val description: String,
    val containingFile: CjFile?,
    val sourceElement: PsiElement?,
)

/**
 * 仓颉只读声明视图。
 *
 * 它为 Analysis API 之外的导航、结构导出和调试工具暴露稳定的只读声明层，
 * 但生命周期仍与生成它的分析上下文保持一致。
 */
interface CaLightDeclaration : CaLifetimeOwner {
    val kind: CaLightDeclarationKind

    val name: String?

    val module: CaModule?

    val annotations: List<CaAnnotation>

    val origin: CaLightDeclarationOrigin
}

/**
 * class-like 声明的只读视图。
 */
interface CaLightClassLikeDeclaration : CaLightDeclaration {
    val classId: ClassId?

    val typeParameters: List<Name>

    val superTypes: List<CaType>

    val members: List<CaLightDeclaration>
}

/**
 * `extend` 声明的只读视图。
 *
 * `extend` 在语义上既不是普通 class-like，也不是 callable，
 * 但它和 class-like 一样拥有成员体、super types 与类型参数，因此在 declaration-view
 * 层需要被提升为独立的一等声明，而不是在 decompiled 视图中被隐式吞掉。
 */
interface CaLightExtendDeclaration : CaLightDeclaration {
    val extendId: String

    val targetClassId: ClassId?

    val extendedType: CaType

    val typeParameters: List<Name>

    val superTypes: List<CaType>

    val members: List<CaLightDeclaration>
}

/**
 * callable 声明的只读视图。
 *
 * 属性、字段、构造器和普通函数统一通过 callable 视图承载，
 * 以保持与当前 `CaCallableSymbol` 公共语义一致。
 */
interface CaLightCallableDeclaration : CaLightDeclaration {
    val callableId: CallableId?

    val signature: CaSignature?
}

/**
 * 仓颉声明视图提供器。
 *
 * 它以 Analysis API 为唯一语义来源，对外暴露：
 * 1. `symbol -> declaration view`
 * 2. `file/module -> declaration view` 集合
 * 3. `package/name -> top-level declaration view` 查找
 */
interface CaLightDeclarationProvider {
    fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration?

    fun getLightDeclarations(file: CjFile, useSiteModule: CaModule? = null): List<CaLightDeclaration>

    fun getLightDeclarations(module: CaModule): List<CaLightDeclaration>

    fun getPackageLightDeclaration(packageFqName: FqName, useSiteModule: CaModule): CaLightDeclaration?

    fun findLightDeclarations(packageFqName: FqName, name: Name, useSiteModule: CaModule): List<CaLightDeclaration>

    companion object {
        fun getInstance(project: Project): CaLightDeclarationProvider = project.service()
    }
}
