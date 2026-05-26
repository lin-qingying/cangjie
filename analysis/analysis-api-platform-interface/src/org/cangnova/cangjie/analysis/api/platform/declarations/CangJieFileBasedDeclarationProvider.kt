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
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.yieldIfNotNull

@CaPlatformInterface
class CangJieFileBasedDeclarationProvider(val cangjieFile: CjFile) : CangJieDeclarationProvider {
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

    private val topLevelDeclarations: Sequence<CjDeclaration>
        get() {
            return sequence {
                for (child in currentCangJieFile.declarations) {

                    yield(child)

                }
            }
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? {
        return getClassLikeDeclarationsByClassId(classId).firstOrNull()
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> {
        return getClassLikeDeclarationsByClassId(classId)
            .filterIsInstance<CjTypeStatement>()
            .filterNot(CjTypeStatement::isExtend)
            .toList()
    }

    private fun getClassLikeDeclarationsByClassId(classId: ClassId): Sequence<CjClassLikeDeclaration> {
        if (filePackageFqName != classId.packageFqName) {
            return emptySequence()
        }

        data class Task(val chunks: List<Name>, val element: PsiElement)

        return sequence {
            val tasks = ArrayDeque<Task>()

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

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<CjTypeAlias> {
        return getClassLikeDeclarationsByClassId(classId).filterIsInstance<CjTypeAlias>().toList()
    }

    override fun getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames<CjClassLikeDeclaration>(packageFqName) { declaration ->
            declaration !is CjExtend
        }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<CjProperty> {
        return getTopLevelCallables(callableId)
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<CjNamedFunction> {
        return getTopLevelCallables(callableId)
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<CjFile> {
        return buildSet {
            getTopLevelProperties(callableId).mapTo(this) { it.containingCjFile }
            getTopLevelFunctions(callableId).mapTo(this) { it.containingCjFile }
        }
    }

    override fun getTopLevelExtends(): Collection<CjExtend> {
        return topLevelDeclarations.filterIsInstance<CjExtend>().toList()
    }

    override fun getTopLevelExtendFiles(): Collection<CjFile> {
        return if (getTopLevelExtends().isNotEmpty()) listOf(currentCangJieFile) else emptyList()
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames<CjCallableDeclaration>(packageFqName)
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> {
        if (filePackageFqName != packageFqName) {
            return emptyList()
        }

        return listOf(currentCangJieFile)
    }

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

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> = emptyList()

    override fun computePackageNames(): Set<String> = setOf(filePackageFqName.asString())

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false


    private inline fun <reified T : CjCallableDeclaration> getTopLevelCallables(callableId: CallableId): Collection<T> {
        require(callableId.classId == null)
        return getTopLevelDeclarations(callableId.packageName, callableId.callableName)
    }

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
