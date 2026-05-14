package org.cangnova.cangjie.analysis.tools

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.api.platform.declarations.createDeclarationProvider
import org.cangnova.cangjie.analysis.api.stubs.CaStubIndexFacade
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationRenderer
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
@OptIn(CaPlatformInterface::class)
/**
 * Analysis 外围模块的统一 inspector 入口。
 *
 * 这组工具只服务内部检查、golden 输出和一致性回归：
 * 1. 反编译文本 dump
 * 2. stub dump
 * 3. declaration view dump
 * 4. 三视图一致性检查
 */
class CaAnalysisInspectorTools(
    private val project: Project,
) {
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    private val decompiledBinaryIndex: CaDecompiledBinaryIndex
        get() = CaDecompiledBinaryIndex.getInstance(project)

    private val stubIndexFacade: CaStubIndexFacade
        get() = CaStubIndexFacade.getInstance(project)

    private val lightDeclarationProvider: CaLightDeclarationProvider
        get() = CaLightDeclarationProvider.getInstance(project)

    fun dumpDecompiledText(module: CaLibraryModule, packageFqName: FqName): String {
        val binaryFile = decompiledBinaryIndex.findBinaryFile(module, packageFqName)
            ?: return "<missing decompiled text for ${packageFqName.asString()}>"
        return (psiManager.findFile(binaryFile) as? CjFile)?.text
            ?: "<missing decompiled text for ${packageFqName.asString()}>"
    }

    fun dumpDecompiledText(module: CaBuiltinsModule, packageFqName: FqName): String {
        val binaryFile = decompiledBinaryIndex.findBinaryFile(module, packageFqName)
            ?: return "<missing decompiled text for ${packageFqName.asString()}>"
        return (psiManager.findFile(binaryFile) as? CjFile)?.text
            ?: "<missing decompiled text for ${packageFqName.asString()}>"
    }

    fun dumpStubFile(file: CjFile): String {
        val kind = stubIndexFacade.fileProvider.getFileStubKind(file)
        val classifiers = stubIndexFacade.fileProvider.getTopLevelClassifierNames(file)
        val callables = stubIndexFacade.fileProvider.getTopLevelCallableNames(file)
        return buildString {
            appendLine("file=${file.name}")
            appendLine("kind=${kind ?: "<missing>"}")
            appendLine("package=${file.packageFqName.asString()}")
            appendLine("topLevelClassifiers=${classifiers.map { it.asString() }.sorted()}")
            appendLine("topLevelCallables=${callables.map { it.asString() }.sorted()}")
        }.trimEnd()
    }

    fun dumpStubPackage(packageFqName: FqName): String {
        val classifiers = stubIndexFacade.packageIndex.getTopLevelClassifierNames(packageFqName)
        val callables = stubIndexFacade.packageIndex.getTopLevelCallableNames(packageFqName)
        return buildString {
            appendLine("package=${packageFqName.asString()}")
            appendLine("topLevelClassifiers=${classifiers.map { it.asString() }.sorted()}")
            appendLine("topLevelCallables=${callables.map { it.asString() }.sorted()}")
        }.trimEnd()
    }

    fun dumpClassMemberStubNames(classId: ClassId): String {
        val names = stubIndexFacade.getClassMemberNames(classId).map { it.asString() }.sorted()
        return "class=${classId.asString()} members=$names"
    }

    fun dumpLightDeclarations(module: CaModule): String {
        val declarations = collectLightDeclarationFiles(module)
            .flatMap { file -> lightDeclarationProvider.getLightDeclarations(file, module) }
        return CaLightDeclarationRenderer.renderTree(declarations)
    }

    fun dumpLightDeclarations(file: CjFile, useSiteModule: CaModule? = null): String {
        return CaLightDeclarationRenderer.renderTree(lightDeclarationProvider.getLightDeclarations(file, useSiteModule))
    }

    fun dumpDecompiledLightDeclarations(module: CaLibraryModule, packageFqName: FqName): String {
        val file = findDecompiledFile(module, packageFqName)
            ?: return "<missing decompiled file for ${packageFqName.asString()}>"
        return dumpLightDeclarations(file, module)
    }

    fun dumpDecompiledLightDeclarations(module: CaBuiltinsModule, packageFqName: FqName): String {
        val file = findDecompiledFile(module, packageFqName)
            ?: return "<missing decompiled file for ${packageFqName.asString()}>"
        return dumpLightDeclarations(file, module)
    }

    fun checkViewConsistency(module: CaLibraryModule, packageFqName: FqName): List<CaAnalysisViewConsistencyIssue> {
        return checkViewConsistencyInternal(module, findDecompiledFile(module, packageFqName), packageFqName)
    }

    fun checkViewConsistency(module: CaBuiltinsModule, packageFqName: FqName): List<CaAnalysisViewConsistencyIssue> {
        return checkViewConsistencyInternal(module, findDecompiledFile(module, packageFqName), packageFqName)
    }

    /**
     * decompiled 一致性检查统一走这一层，确保 library / builtins 使用同一套判定语义。
     *
     * 这里不仅检查 “stub 中有但 light declaration 丢了”，
     * 也检查 “light declaration 多投影了 stub 中不存在的名字”，
     * 方便在迁移过程中及时发现索引层与声明视图层的漂移。
     */
    private fun checkViewConsistencyInternal(
        module: CaModule,
        decompiledFile: CjFile?,
        packageFqName: FqName,
    ): List<CaAnalysisViewConsistencyIssue> {
        decompiledFile ?: return listOf(
            CaAnalysisViewConsistencyIssue(
                kind = CaAnalysisViewConsistencyIssueKind.MISSING_DECOMPILED_FILE,
                message = "Missing decompiled file for ${packageFqName.asString()}",
            ),
        )

        val stubClassifiers = stubIndexFacade.fileProvider.getTopLevelClassifierNames(decompiledFile).map { it.asString() }.toSet()
        val stubCallables = stubIndexFacade.fileProvider.getTopLevelCallableNames(decompiledFile).map { it.asString() }.toSet()
        val declarationViews = lightDeclarationProvider.getLightDeclarations(decompiledFile, module)
        val declarationViewClassifiers = declarationViews.filterIsInstance<CaLightClassLikeDeclaration>()
            .mapNotNull(CaLightDeclaration::name)
            .toSet()
        val declarationViewExtends = declarationViews.filterIsInstance<CaLightExtendDeclaration>()
            .map(CaLightExtendDeclaration::extendId)
            .toSet()
        val declarationViewCallables = declarationViews.filterIsInstance<CaLightCallableDeclaration>()
            .mapNotNull(CaLightDeclaration::name)
            .toSet()
        val psiExtendIds = decompiledFile.declarations
            .filterIsInstance<CjExtend>()
            .map(CjExtend::getExtendId)
            .toSet()

        return buildList {
            collectSetMismatch(
                expected = stubClassifiers,
                actual = declarationViewClassifiers,
                missingKind = CaAnalysisViewConsistencyIssueKind.MISSING_LIGHT_DECLARATION,
                unexpectedKind = CaAnalysisViewConsistencyIssueKind.UNEXPECTED_LIGHT_DECLARATION,
                label = "class-like",
            ).forEach(::add)

            collectSetMismatch(
                expected = stubCallables,
                actual = declarationViewCallables,
                missingKind = CaAnalysisViewConsistencyIssueKind.MISSING_LIGHT_DECLARATION,
                unexpectedKind = CaAnalysisViewConsistencyIssueKind.UNEXPECTED_LIGHT_DECLARATION,
                label = "callable",
            ).forEach(::add)

            collectSetMismatch(
                expected = psiExtendIds,
                actual = declarationViewExtends,
                missingKind = CaAnalysisViewConsistencyIssueKind.MISSING_LIGHT_DECLARATION,
                unexpectedKind = CaAnalysisViewConsistencyIssueKind.UNEXPECTED_LIGHT_DECLARATION,
                label = "extend",
            ).forEach(::add)
        }
    }

    /**
     * 把“缺失”和“多出”两类差异统一规范化成 issue。
     *
     * 这样 analysis-tools 的 golden / dump 输出可以稳定地区分：
     * - 索引层漏投影
     * - 声明视图层误投影
     */
    private fun collectSetMismatch(
        expected: Set<String>,
        actual: Set<String>,
        missingKind: CaAnalysisViewConsistencyIssueKind,
        unexpectedKind: CaAnalysisViewConsistencyIssueKind,
        label: String,
    ): List<CaAnalysisViewConsistencyIssue> {
        val missing = expected - actual
        val unexpected = actual - expected
        return buildList {
            if (missing.isNotEmpty()) {
                add(
                    CaAnalysisViewConsistencyIssue(
                        kind = missingKind,
                        message = "Missing declaration-view $label entries: ${missing.sorted()}",
                    ),
                )
            }
            if (unexpected.isNotEmpty()) {
                add(
                    CaAnalysisViewConsistencyIssue(
                        kind = unexpectedKind,
                        message = "Unexpected declaration-view $label entries: ${unexpected.sorted()}",
                    ),
                )
            }
        }
    }

    private fun collectLightDeclarationFiles(module: CaModule): List<CjFile> {
        val declarationProvider = project.createDeclarationProvider(module.contentScope, module)
        val sourceFiles = declarationProvider.computePackageNames()
            .orEmpty()
            .flatMap { packageName ->
                val packageFqName = FqName(packageName)
                collectPackageSourceFiles(declarationProvider, packageFqName)
            }
        val decompiledFiles = collectDecompiledFiles(module)
        return (sourceFiles + decompiledFiles).distinctBy { file -> file.virtualFile ?: file }
    }

    private fun collectPackageSourceFiles(
        declarationProvider: org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider,
        packageFqName: FqName,
    ): List<CjFile> {
        val classifierNames = declarationProvider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName)
        val callableNames = declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)

        return buildList {
            addAll(declarationProvider.findFilesForFacadeByPackage(packageFqName))
            classifierNames.forEach { name ->
                val classId = ClassId(packageFqName, name)
                declarationProvider.getAllClassesByClassId(classId).mapTo(this) { it.containingCjFile }
                declarationProvider.getAllTypeAliasesByClassId(classId).mapTo(this) { it.containingCjFile }
            }
            callableNames.forEach { name ->
                addAll(declarationProvider.getTopLevelCallableFiles(CallableId(packageFqName, name)))
            }
        }.distinctBy { file -> file.virtualFile ?: file }
    }

    private fun collectDecompiledFiles(module: CaModule): List<CjFile> {
        return when (module) {
            is CaLibraryModule -> decompiledBinaryIndex.getBinaryFiles(module).mapNotNull(psiManager::findFile).filterIsInstance<CjFile>()
            is CaBuiltinsModule -> decompiledBinaryIndex.getBinaryFiles(module).mapNotNull(psiManager::findFile).filterIsInstance<CjFile>()
            else -> emptyList()
        }
    }

    private fun findDecompiledFile(module: CaLibraryModule, packageFqName: FqName): CjFile? {
        val binaryFile = decompiledBinaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return psiManager.findFile(binaryFile) as? CjFile
    }

    private fun findDecompiledFile(module: CaBuiltinsModule, packageFqName: FqName): CjFile? {
        val binaryFile = decompiledBinaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return psiManager.findFile(binaryFile) as? CjFile
    }
}

enum class CaAnalysisViewConsistencyIssueKind {
    MISSING_DECOMPILED_FILE,
    MISSING_LIGHT_DECLARATION,
    UNEXPECTED_LIGHT_DECLARATION,
}

data class CaAnalysisViewConsistencyIssue(
    val kind: CaAnalysisViewConsistencyIssueKind,
    val message: String,
)
