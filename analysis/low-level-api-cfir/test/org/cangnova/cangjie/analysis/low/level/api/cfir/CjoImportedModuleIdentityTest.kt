@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleStateModificationKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionInvalidationService
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories.LLBinaryOriginLibrarySymbolProviderFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.serialization.deserialize.*
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

/** 在现有 headless Analysis 测试框架内使用真实 LL session/factory；PSI 只用于构造 binary-root 描述。 */
class CjoImportedModuleIdentityTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun cjoImportedModuleIdentity(mainFile: CjFile) {
        val project = mainFile.project
        val platform = CangJieProjectStructureProvider.getInstance(project).getModule(mainFile, null).targetPlatform
        val directory = Files.createTempDirectory("ll-cjo-module-identity-")
        try {
            val consumerRoot = Files.createDirectory(directory.resolve("consumer"))
            val targetRoot = Files.createDirectory(directory.resolve("target"))
            fun copy(packageName: String, target: Path) {
                val encoded = Files.readString(Path.of(
                    "cfir/cfir-serialization/testResources/cjo-wire/" + packageName + ".cjo.b64"), Charsets.UTF_8)
                Files.write(target.resolve(packageName + ".cjo"), Base64.getDecoder().decode(encoded.trim()))
            }
            copy("wire.imported", consumerRoot)
            copy("wire.reentry", consumerRoot)
            copy("wire.current", targetRoot)
            val targetModule = Library(project, platform, "cjo-actual-target", psiRoot(project, targetRoot), emptyList())
            val consumerModule = Library(project, platform, "cjo-actual-consumer", psiRoot(project, consumerRoot), listOf(targetModule))
            val cache = LLCfirSessionCache.getInstance(project)
            val consumerSession = cache.getSession(consumerModule, preferBinary = true)
            val targetSession = cache.getSession(targetModule, preferBinary = true)
            val factory = LLBinaryOriginLibrarySymbolProviderFactory
            val consumer = factory.createCommonLibrarySymbolProvider(consumerSession, Any(), consumerModule.contentScope)
                .single() as CfirDeserializedSymbolProvider
            val target = factory.createCommonLibrarySymbolProvider(targetSession, Any(), targetModule.contentScope)
                .single() as CfirDeserializedSymbolProvider
            assertSame(consumerSession.cjoSymbolProvider, consumer)
            assertSame(targetSession.cjoSymbolProvider, target)
            assertSame(consumer, factory.createLibraryExtendProvider(consumerSession))
            assertSame(target, factory.createJvmLibrarySymbolProvider(targetSession, Any(), targetModule.contentScope).single())

            val imported = consumer.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL)!!
            val root = CfirDeclDeserializer(imported, CfirTypeDeserializer(imported)).deserializeDecl(0) as CfirPatternVariable
            val reference = (root.initializer as CfirNamedAccessExpression).calleeReference as CfirResolvedNamedReference
            val named = target.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single()
            assertSame(named, reference.resolvedSymbol)
            assertSame(targetSession.moduleData, named.cfir.moduleData)
            assertSame(target.getOwnPackageContext("wire.current", CjoDeserializationMode.FULL),
                consumer.getPackageContext("wire.current", CjoDeserializationMode.FULL))
            assertFalse(consumer.hasPackage(FqName("wire.current")))
            assertFalse(consumer.hasPackage(FqName("std.core")))

            val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
            val sdk = factory.createBuiltinsSymbolProvider(builtinsSession).first() as CfirDeserializedSymbolProvider
            assertSame(builtinsSession.cjoSymbolProvider, sdk)
            assertSame(sdk, factory.createBuiltinsExtendProvider(builtinsSession))
            val annotationContext = consumer.getOwnPackageContext("wire.reentry", CjoDeserializationMode.FULL)!!
            val markIndex = annotationContext.header.topLevelClassifierNameToIndices.getValue("Mark").single()
            val mark = CfirDeclDeserializer(annotationContext, CfirTypeDeserializer(annotationContext))
                .deserializeDecl(markIndex) as CfirClass
            val array = (mark.annotations.single().arguments.single() as CfirNamedArgumentExpression).expression as CfirArrayLiteral
            val sdkReference = (array.elements.single() as CfirNamedAccessExpression).calleeReference as CfirResolvedNamedReference
            val enum = sdk.getClassLikeSymbolByClassId(ClassId(FqName("std.core"), Name.identifier("AnnotationKind")))!!.cfir as CfirEnum
            val constructor = enum.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Type" }.symbol
            assertSame(constructor, sdkReference.resolvedSymbol)
            assertSame(builtinsSession.moduleData, constructor.cfir.moduleData)
            assertSame(sdk.getOwnPackageContext("std.core", CjoDeserializationMode.SIGNATURE_ONLY),
                consumer.getPackageContext("std.core", CjoDeserializationMode.SIGNATURE_ONLY))
            assertNotSame(sdk.getOwnPackageContext("std.core", CjoDeserializationMode.FULL),
                consumer.getPackageContext("std.core", CjoDeserializationMode.SIGNATURE_ONLY))

            val duplicate = Library(project, platform, "cjo-duplicate-target", psiRoot(project, targetRoot), emptyList())
            val ambiguous = Library(project, platform, "cjo-ambiguous-consumer", psiRoot(project, consumerRoot), listOf(targetModule, duplicate))
            val ambiguousProvider = cache.getSession(ambiguous, preferBinary = true).cjoSymbolProvider
            assertThrows(IllegalStateException::class.java) {
                ambiguousProvider.getPackageContext("wire.current", CjoDeserializationMode.FULL)
            }

            ApplicationManager.getApplication().runWriteAction {
                LLCfirSessionInvalidationService.getInstance(project).invalidate(
                    KotlinModuleStateModificationEvent(targetModule, KotlinModuleStateModificationKind.UPDATE))
            }
            assertFalse(consumerSession.isValid)
            assertFalse(targetSession.isValid)
            val freshConsumer = cache.getSession(consumerModule, preferBinary = true).cjoSymbolProvider
            val freshTarget = cache.getSession(targetModule, preferBinary = true).cjoSymbolProvider
            val freshContext = freshConsumer.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL)!!
            val freshRoot = CfirDeclDeserializer(freshContext, CfirTypeDeserializer(freshContext)).deserializeDecl(0) as CfirPatternVariable
            val freshReference = (freshRoot.initializer as CfirNamedAccessExpression).calleeReference as CfirResolvedNamedReference
            val freshNamed = freshTarget.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single()
            assertNotSame(named, freshNamed)
            assertSame(freshNamed, freshReference.resolvedSymbol)
        } finally { directory.toFile().deleteRecursively() }
    }

    private fun psiRoot(project: Project, path: Path): PsiFileSystemItem {
        val file = StandardFileSystems.local().refreshAndFindFileByPath(path.toAbsolutePath().toString().replace('\\', '/'))!!
        return PsiManager.getInstance(project).findDirectory(file)!!
    }

    private class Library(
        override val project: Project,
        override val targetPlatform: TargetPlatform,
        override val libraryName: String,
        root: PsiFileSystemItem,
        override val directRegularDependencies: List<CaModule>,
    ) : CaLibraryModule {
        override val binaryRoots = listOf(root)
        override val baseContentScope = GlobalSearchScopesCore.directoryScope(project, root.virtualFile, true)
        override val contentScope = baseContentScope
    }
}
