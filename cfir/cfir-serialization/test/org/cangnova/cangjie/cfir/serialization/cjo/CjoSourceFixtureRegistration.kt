@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession

/** 直接构造的源码 fixture 也由正式 construction 产品完成一次性文件、声明及符号归属登记。 */
internal fun registerCjoSourceFixture(file: CfirFile) {
    val session = file.moduleData.session
    val provider = CfirProviderImpl(session, CfirCangJieScopeProvider())
    session.register(CfirProvider::class, provider)
    session.register(CfirSymbolProvider::class, provider.symbolProvider)
    val pre = buildPreMacroRawFiles(
        session, listOf(PreMacroCfirFile(file, annotationMetadata = CfirAnnotationMetadataBuilder())),
    )
    val result = MacroConstructionService.successOf(pre, listOf(file), MacroExpansionRegistry.EMPTY)
    recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
}

/** 依赖 provider 与本单元源码 provider 组合；安装库依赖不能替换源码的名称和声明索引。 */
internal fun registerCjoSourceFixtureDependencies(session: CfirSession, dependencies: CfirSymbolProvider) {
    val sourceProvider = session.cfirProvider as CfirProviderImpl
    session.register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencies)
    session.register(CfirSymbolProvider::class, CfirCompositeSymbolProvider(session, listOf(sourceProvider.symbolProvider, dependencies)))
}
