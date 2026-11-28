package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.source.importedSourceGraphProvider
import java.util.Collections
import java.util.IdentityHashMap

/**
 * specific 前端明确请求本包 CJMP family 的已保存 SOURCE 正文。
 * header、普通正文和 CFG 就绪互相独立；这里只沿原 provider 完成内容事务，
 * 不在 scope/checker/getter 中加载，不扫描其它 SDK 包，也不重新解析 Library 源码。
 */
internal fun prepareCjmpImportedSourceContent(output: SingleModuleFrontendOutput) {
    if (!output.session.cjmpSettings.compileSpecific) return
    val visited = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
    fun owningMembers(owner: CfirDeclaration, physicalMembers: List<CfirDeclaration>, family: CfirCjmpFamilySnapshot): List<CfirDeclaration> {
        if (!family.hasMemberOwner(owner)) return physicalMembers
        val merged = family.membersFor(owner)
        val mergedSymbols = merged.mapTo(hashSetOf()) { it.symbol }
        // 与 declared/header 准备域一致：family 合并成员之外，仅加入实际后生成声明，
        // 不把被替换的 common/specific 物理成员重新认领为当前逻辑 owner 的内容。
        return merged + physicalMembers.filter { it.exportFacts.isCompilerAdded && it.symbol !in mergedSymbols }
    }
    fun prepare(declaration: CfirDeclaration, family: CfirCjmpFamilySnapshot) {
        if (!visited.add(declaration)) return
        if (declaration.cjoDeclarationOrigin != null) {
            val provider = checkNotNull(declaration.moduleData.session.importedSourceGraphProvider)
            check(provider.sourceHeaderContentsOf(declaration)) { "The selected header content must belong to its actual provider" }
            if (declaration is CfirVariable && declaration !is CfirValueParameter)
                provider.sourceInitializerOf(declaration)
        }
        if (declaration.cjoDeclarationOrigin != null && declaration is CfirFunctionBodyOwner) {
            val callable = declaration as CfirCallableDeclaration
            val provider = checkNotNull(declaration.moduleData.session.importedSourceGraphProvider) {
                "A loaded CJMP SOURCE declaration requires its owning content provider"
            }
            val header = provider.sourceHeaderOf(callable)
            // 无参数 enum 构造器保存为 VarDecl，没有 F/header/body；不为它生成正文。
            check(header != null || callable is CfirEnumConstructor)
            if (header?.bodyAvailable == true) checkNotNull(provider.sourceBodyOf(declaration)) {
                "The selected saved SOURCE body must be published by its original owner"
            }
        }
        when (declaration) {
            is CfirClassLikeDeclaration -> owningMembers(declaration, declaration.declarations, family).forEach { prepare(it, family) }
            is CfirExtend -> owningMembers(declaration, declaration.declarations, family).forEach { prepare(it, family) }
            is CfirProperty -> {
                declaration.getter?.let { prepare(it, family) }
                declaration.setter?.let { prepare(it, family) }
            }
            is CfirFunction -> declaration.valueParameters.forEach { parameter ->
                parameter.defaultValueMetadata?.function?.let { prepare(it, family) }
            }
            is CfirEnumConstructor -> declaration.valueParameters.forEach { parameter ->
                parameter.defaultValueMetadata?.function?.let { prepare(it, family) }
            }
            else -> Unit // 其它声明不拥有函数正文、accessor 或参数 helper。
        }
    }
    for (packageName in output.fir.map { it.packageDirective.packageFqName }.distinct()) {
        val family = output.session.cjmpFamilyAggregate.ensureFamilyComplete(packageName)
        family.packageDeclarationsForExport().forEach { prepare(it, family) }
    }
}
