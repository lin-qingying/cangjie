package org.cangnova.cangjie.analysis.api.platform.declarations

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.fileClasses.cangjieFileFacadeFqName
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationContainer
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.yieldIfNotNull

/**
 * 基于单个 [CjFile] 的声明 provider。
 */
@CaPlatformInterface
class CangJieFileBasedDeclarationProvider(
    /**
     * 作为声明查询来源的仓颉文件。
     */
    val cangjieFile: CjFile,
) : CangJieDeclarationProvider {
    /**
     * 用于从 VirtualFile 恢复最新 PSI 的 PSI manager。
     */
    private val psiManager: PsiManager = PsiManager.getInstance(cangjieFile.project)

    /**
     * decompiled `.cjo` 的 PSI provider 在 IDE 生命周期中可能被重建。
     *
     * declaration provider 不能长期持有第一次收集到的 compiled PSI，
     * 否则后续再命中同一 VirtualFile 时就会落到 “different providers” 的陈旧 PSI。
     */
    private val currentCangJieFile: CjFile
        get() {
            if (!cangjieFile.isCompiled) {
                return cangjieFile
            }

            val virtualFile = checkNotNull(cangjieFile.virtualFile) {
                "Compiled CangJie file should always have a virtual file: $cangjieFile"
            }
            return checkNotNull(psiManager.findFile(virtualFile) as? CjFile) {
                "Cannot restore compiled CangJie PSI for ${virtualFile.path}"
            }
        }

    /**
     * compiled `.cjo` 的 package 判断必须绑定到当前 live PSI。
     *
     * `CjFile.packageFqName` 本身已经优先走 green stub，
     * 这里只需要避免继续读取陈旧 PSI，不能再强制 `calcStubTree()`。
     */
    private val filePackageFqName: FqName
        get() {
            val file = currentCangJieFile
            return file.packageFqName
        }

    /**
     * 当前文件的顶层声明序列。
     */
    private val topLevelDeclarations: Sequence<CjDeclaration>
        get() {
            return sequence {
                for (child in currentCangJieFile.declarations) {

                    yield(child)

                }
            }
        }

    /**
     * 查找指定 class id 对应的首个类状声明。
     */
    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? {
        return getClassLikeDeclarationsByClassId(classId).firstOrNull()
    }

    /**
     * 查找指定 class id 对应的所有普通类声明，排除 extend。
     */
    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> {
        return getClassLikeDeclarationsByClassId(classId)
            .filterIsInstance<CjTypeStatement>()
            .filterNot(CjTypeStatement::isExtend)
            .toList()
    }

    /**
     * 递归查找指定 class id 对应的类状声明序列。
     */
    private fun getClassLikeDeclarationsByClassId(classId: ClassId): Sequence<CjClassLikeDeclaration> {
        if (filePackageFqName != classId.packageFqName) {
            return emptySequence()
        }

        data class Task(val chunks: List<Name>, val element: PsiElement)

        return sequence {
            /**
             * 待处理的相对类名片段与 PSI 元素队列。
             */
            val tasks = ArrayDeque<Task>()

            /**
             * 从 class id 中拆出的相对类名片段。
             */
            val startingChunks = classId.relativeClassName.pathSegments()
            for (declaration in topLevelDeclarations) {
                tasks.addLast(Task(startingChunks, declaration))
            }

            tasks += Task(startingChunks, currentCangJieFile)

            while (!tasks.isEmpty()) {
                val (chunks, element) = tasks.removeFirst()
                assert(chunks.isNotEmpty())

                if (element !is CjNamedDeclaration || element.nameAsName != chunks[0]) {
                    continue
                }

                if (chunks.size == 1) {
                    yieldIfNotNull((element as? CjClassLikeDeclaration)?.takeUnless { it is CjExtend })
                    continue
                }

                if (element is CjDeclarationContainer) {
                    val newChunks = chunks.subList(1, chunks.size)
                    for (child in element.declarations) {
                        tasks.addLast(Task(newChunks, child))
                    }
                }
            }
        }
    }

    /**
     * 查找指定 class id 对应的所有类型别名。
     */
    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias> {
        return getClassLikeDeclarationsByClassId(classId).filterIsInstance<CjTypeAlias>().toList()
    }

    /**
     * 获取当前文件所属包中所有顶层类状声明名称。
     */
    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames<CjClassLikeDeclaration>(packageFqName) { declaration ->
            declaration !is CjExtend
        }
    }

    /**
     * 获取指定 callable id 对应的顶层属性。
     */
    override fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty> {
        return getTopLevelCallables(callableId)
    }

    /**
     * 获取指定 callable id 对应的顶层函数。
     */
    override fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction> {
        return getTopLevelCallables(callableId)
    }

    /**
     * 获取指定 callable id 对应的顶层宏。
     */
    override fun getTopLevelMacros(callableId: CallableId): Collection<CjMacroDeclaration> {
        return getTopLevelCallables(callableId)
    }

    /**
     * 获取指定 callable id 对应顶层 callable 所在文件。
     */
    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile> {
        return buildSet {
            getTopLevelProperties(callableId).mapTo(this) { it.containingCjFile }
            getTopLevelFunctions(callableId).mapTo(this) { it.containingCjFile }
            getTopLevelMacros(callableId).mapTo(this) { it.containingCjFile }
        }
    }

    /**
     * 获取当前文件中的顶层 extend 声明。
     */
    override fun getTopLevelExtends(): Collection<CjExtend> {
        return topLevelDeclarations.filterIsInstance<CjExtend>().toList()
    }

    /**
     * 当前文件存在顶层 extend 时返回该文件。
     */
    override fun getTopLevelExtendFiles(): Collection<CjFile> {
        return if (getTopLevelExtends().isNotEmpty()) listOf(currentCangJieFile) else emptyList()
    }

    /**
     * 获取指定包中的顶层 callable 名称。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames<CjCallableDeclaration>(packageFqName)
    }

    /**
     * 按包名查找当前文件是否参与 facade。
     */
    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> {
        if (filePackageFqName != packageFqName) {
            return emptyList()
        }

        return listOf(currentCangJieFile)
    }

    /**
     * 按 facade 完整名查找当前文件。
     */
    override fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        val file = currentCangJieFile
        if (file.cangjieFileFacadeFqName != facadeFqName) return emptyList()

        for (declaration in topLevelDeclarations) {
            if (declaration !is CjClassLikeDeclaration) {
                return listOf(file)
            }
        }

        return emptyList()
    }

    /**
     * 单文件 provider 不提供额外内部 facade 文件。
     */
    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> = emptyList()

    /**
     * 返回当前文件所属包名。
     */
    override fun computePackageNames(): Set<String> = setOf(filePackageFqName.asString())

    /**
     * 单文件 provider 不区分 classifier 包名计算。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    /**
     * 单文件 provider 不区分 callable 包名计算。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false


    /**
     * 获取指定 callable id 对应的顶层 callable 声明。
     */
    private inline fun <reified T : CjCallableDeclaration> getTopLevelCallables(callableId: CallableId): Collection<T> {
        require(callableId.classId == null)
        return getTopLevelDeclarations(callableId.packageName, callableId.callableName)
    }

    /**
     * 获取指定包名和名称对应的顶层命名声明。
     */
    private inline fun <reified T : CjNamedDeclaration> getTopLevelDeclarations(
        packageFqName: FqName,
        name: Name
    ): Collection<T> {
        if (filePackageFqName != packageFqName) {
            return emptyList()
        }

        return buildList {
            for (declaration in topLevelDeclarations) {
                if (declaration is T && declaration.nameAsName == name) {
                    add(declaration)
                }
            }
        }
    }

    /**
     * 获取指定包中满足 [predicate] 的顶层命名声明名称。
     */
    private inline fun <reified T : CjNamedDeclaration> getTopLevelDeclarationNames(
        packageFqName: FqName,
        predicate: (T) -> Boolean = { true },
    ): Set<Name> {
        if (filePackageFqName != packageFqName) {
            return emptySet()
        }

        return buildSet {
            for (declaration in topLevelDeclarations) {
                if (declaration is T && predicate(declaration)) {
                    addIfNotNull(declaration.nameAsName)
                }
            }
        }
    }
}
