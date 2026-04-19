package org.cangnova.cangjie.analysis.api.platform.declarations

import com.intellij.psi.PsiElement
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
    private val topLevelDeclarations: Sequence<CjDeclaration>
        get() {
            return sequence {
                for (child in cangjieFile.declarations) {

                    yield(child)

                }
            }
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): CjClassLikeDeclaration? {
        return getClassLikeDeclarationsByClassId(classId).firstOrNull()
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<CjTypeStatement> {
        return getClassLikeDeclarationsByClassId(classId).filterIsInstance<CjTypeStatement>().toList()
    }

    private fun getClassLikeDeclarationsByClassId(classId: ClassId): Sequence<CjClassLikeDeclaration> {
        if (cangjieFile.packageFqName != classId.packageFqName) {
            return emptySequence()
        }

        data class Task(val chunks: List<Name>, val element: PsiElement)

        return sequence {
            val tasks = ArrayDeque<Task>()

            val startingChunks = classId.relativeClassName.pathSegments()
            for (declaration in topLevelDeclarations) {
                tasks.addLast(Task(startingChunks, declaration))
            }

            tasks += Task(startingChunks, cangjieFile)

            while (!tasks.isEmpty()) {
                val (chunks, element) = tasks.removeFirst()
                assert(chunks.isNotEmpty())

                if (element !is CjNamedDeclaration || element.nameAsName != chunks[0]) {
                    continue
                }

                if (chunks.size == 1) {
                    yieldIfNotNull(element as? CjClassLikeDeclaration)
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
        return getTopLevelDeclarationNames<CjClassLikeDeclaration>(packageFqName)
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

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames<CjCallableDeclaration>(packageFqName)
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<CjFile> {
        if (cangjieFile.packageFqName != packageFqName) {
            return emptyList()
        }

        return listOf(cangjieFile)
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<CjFile> {
        if (cangjieFile.cangjieFileFacadeFqName != facadeFqName) return emptyList()

        for (declaration in topLevelDeclarations) {
            if (declaration !is CjClassLikeDeclaration) {
                return listOf(cangjieFile)
            }
        }

        return emptyList()
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<CjFile> = emptyList()

    override fun computePackageNames(): Set<String> = setOf(cangjieFile.packageFqName.asString())

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
        if (cangjieFile.packageFqName != packageFqName) {
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

    private inline fun <reified T : CjNamedDeclaration> getTopLevelDeclarationNames(packageFqName: FqName): Set<Name> {
        if (cangjieFile.packageFqName != packageFqName) {
            return emptySet()
        }

        return buildSet {
            for (declaration in topLevelDeclarations) {
                if (declaration is T) {
                    addIfNotNull(declaration.nameAsName)
                }
            }
        }
    }
}
