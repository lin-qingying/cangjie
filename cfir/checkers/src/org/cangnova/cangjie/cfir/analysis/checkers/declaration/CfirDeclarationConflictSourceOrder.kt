package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.isInSameSourceTree
import java.util.IdentityHashMap

/**
 * 为 CFIR 声明冲突诊断提供可证明的源码顺序。
 *
 * 声明宏输出来自独立解析树，原始 offset 只能在该 fragment 内解释。诊断代表顺序先使用
 * registry 把生成 source 归一到宏调用 source；同一 CFIR 文件按最终声明树顺序比较，
 * 跨文件先按 cjc 的文件 basename 顺序比较；basename 相同时用完整路径建立 IDE 内稳定的诊断锚点。
 */
internal class CfirDeclarationConflictSourceOrder(
    private val session: CfirSession,
) {
    private data class Position(
        val rawSource: AbstractCjSourceElement,
        val logicalSource: AbstractCjSourceElement,
    )

    private val positions = IdentityHashMap<CfirBasedSymbol<*>, Position>()
    private val declarationOrderByFile = IdentityHashMap<CfirFile, IdentityHashMap<CfirDeclaration, Int>>()

    /** 仅使用共同源码树、同一 CFIR 文件声明顺序或同包文件路径作为顺序证据。 */
    fun compareKnown(first: CfirBasedSymbol<*>, second: CfirBasedSymbol<*>): Int? {
        if (first === second) return 0
        val firstPosition = position(first)
        val secondPosition = position(second)

        if (firstPosition != null && secondPosition != null) {
            compareWithinTree(firstPosition.logicalSource, secondPosition.logicalSource)
                ?.takeIf { it != 0 }
                ?.let { return it }
            compareWithinTree(firstPosition.rawSource, secondPosition.rawSource)
                ?.takeIf { it != 0 }
                ?.let { return it }
        }
        compareDeclarationOrder(first, second)
            ?.takeIf { it != 0 }
            ?.let { return it }
        compareFilePath(first, second)
            ?.takeIf { it != 0 }
            ?.let { return it }
        return null
    }

    /**
     * 为同签名冲突簇选择唯一、稳定的 IDE 诊断代表。
     *
     * 已知的源码顺序优先；当两个声明映射到同一宏调用位置或缺少共同 source tree 时，
     * 使用源码文件、宏展开片段位置及最终 CFIR 声明槽位建立确定性平局顺序。
     */
    fun compareForDiagnosticRepresentative(first: CfirBasedSymbol<*>, second: CfirBasedSymbol<*>): Int {
        if (first === second) return 0
        compareKnown(first, second)?.takeIf { it != 0 }?.let { return it }
        return stableKey(first).compareTo(stableKey(second))
    }

    private fun position(symbol: CfirBasedSymbol<*>): Position? {
        positions[symbol]?.let { return it }
        if (!symbol.isBound) return null

        val rawSource = sourceDeclaration(symbol).cfir.source ?: return null
        val mappedSource = session.macroExpansionRegistry?.originSourceForGeneratedSource(rawSource)
        return Position(
            rawSource = rawSource,
            logicalSource = mappedSource ?: rawSource,
        ).also { positions[symbol] = it }
    }

    private fun stableKey(symbol: CfirBasedSymbol<*>): StableDeclarationKey {
        val sourceDeclaration = sourceDeclaration(symbol)
        val declaration = sourceDeclaration.takeIf { it.isBound }?.cfir
        val source = declaration?.source
        val file = declaration?.let { sourceDeclaration.getContainingFile() ?: symbol.getContainingFile() }
        val path = file?.sourceFile?.path.orEmpty().replace('\\', '/')
        val declarationIndex = declaration?.let { boundDeclaration ->
            file?.let { containingFile -> declarationOrder(containingFile, boundDeclaration) }
        } ?: -1
        val callable = symbol as? CfirCallableSymbol<*>
        val containingClass = callable
            ?.takeIf { it.isBound }
            ?.getContainingClass()
            ?.classId
            ?.asString()
            .orEmpty()
        return StableDeclarationKey(
            sourcePath = path,
            sourceFileName = file?.sourceFile?.name.orEmpty(),
            packageName = file?.packageDirective?.packageFqName?.asString().orEmpty(),
            sourceStart = source?.startOffset ?: -1,
            declarationIndex = declarationIndex,
            containingClass = containingClass,
            declarationKind = declaration?.let { it::class.qualifiedName }.orEmpty()
                .ifEmpty { sourceDeclaration::class.qualifiedName.orEmpty() },
            origin = if (symbol.isBound) symbol.origin.toString() else "unbound",
            callableId = callable?.callableId?.toString().orEmpty(),
            presentation = CfirRedeclarationPresenter.represent(symbol).orEmpty(),
        )
    }

    private fun sourceDeclaration(symbol: CfirBasedSymbol<*>): CfirBasedSymbol<*> =
        if (symbol.isBound) {
            (symbol as? CfirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol ?: symbol
        } else {
            symbol
        }

    private fun declarationOrder(file: CfirFile, declaration: CfirDeclaration): Int {
        return declarationOrderByFile.getOrPut(file) {
            var nextIndex = 0
            IdentityHashMap<CfirDeclaration, Int>().also { order ->
                file.accept(
                    object : CfirDefaultVisitor<Unit, Unit>() {
                        override fun visitElement(element: CfirElement, data: Unit) {
                            if (element is CfirDeclaration) {
                                order.putIfAbsent(element, nextIndex++)
                            }
                            element.acceptChildren(this, data)
                        }
                    },
                    Unit,
                )
            }
        }[declaration] ?: -1
    }

    private fun compareWithinTree(first: AbstractCjSourceElement, second: AbstractCjSourceElement): Int? {
        val firstSource = first as? CjSourceElement ?: return null
        val secondSource = second as? CjSourceElement ?: return null
        if (!firstSource.isInSameSourceTree(secondSource)) return null
        return firstSource.startOffset.compareTo(secondSource.startOffset)
    }

    private fun compareFilePath(first: CfirBasedSymbol<*>, second: CfirBasedSymbol<*>): Int? {
        if (!first.isBound || !second.isBound) return null
        val firstFile = sourceDeclaration(first).getContainingFile() ?: return null
        val secondFile = sourceDeclaration(second).getContainingFile() ?: return null
        if (firstFile === secondFile) return null
        val ownerSession = firstFile.moduleData.session
        if (ownerSession !== secondFile.moduleData.session) return null

        val packageName = firstFile.packageDirective.packageFqName
        if (packageName != secondFile.packageDirective.packageFqName) return null

        val firstSourceFile = firstFile.sourceFile ?: return null
        val secondSourceFile = secondFile.sourceFile ?: return null
        val firstName = firstSourceFile.name
        val secondName = secondSourceFile.name
        // cjc 的常规编译顺序按大小写敏感的 basename 比较。相同 basename 的路径比较仅为
        // IDE 提供确定性的本地 tie-break；官方编译器不保证同名文件间的主诊断位置。
        val nameComparison = firstName.compareTo(secondName)
        if (nameComparison != 0) return nameComparison

        val firstPath = firstSourceFile.path ?: return null
        val secondPath = secondSourceFile.path ?: return null
        val caseInsensitive = firstPath.compareTo(secondPath, ignoreCase = true)
        return if (caseInsensitive != 0) caseInsensitive else firstPath.compareTo(secondPath)
    }

    private fun compareDeclarationOrder(first: CfirBasedSymbol<*>, second: CfirBasedSymbol<*>): Int? {
        if (!first.isBound || !second.isBound) return null
        val firstDeclaration = sourceDeclaration(first)
        val secondDeclaration = sourceDeclaration(second)
        val firstFile = firstDeclaration.getContainingFile() ?: return null
        val secondFile = secondDeclaration.getContainingFile() ?: return null
        if (firstFile !== secondFile) return null

        val firstIndex = declarationOrder(firstFile, firstDeclaration.cfir).takeIf { it >= 0 } ?: return null
        val secondIndex = declarationOrder(secondFile, secondDeclaration.cfir).takeIf { it >= 0 } ?: return null
        return firstIndex.compareTo(secondIndex)
    }

    private data class StableDeclarationKey(
        val sourcePath: String,
        val sourceFileName: String,
        val packageName: String,
        val sourceStart: Int,
        val declarationIndex: Int,
        val containingClass: String,
        val declarationKind: String,
        val origin: String,
        val callableId: String,
        val presentation: String,
    ) : Comparable<StableDeclarationKey> {
        override fun compareTo(other: StableDeclarationKey): Int = compareValuesBy(
            this,
            other,
            StableDeclarationKey::sourcePath,
            StableDeclarationKey::sourceFileName,
            StableDeclarationKey::packageName,
            StableDeclarationKey::sourceStart,
            StableDeclarationKey::declarationIndex,
            StableDeclarationKey::containingClass,
            StableDeclarationKey::declarationKind,
            StableDeclarationKey::origin,
            StableDeclarationKey::callableId,
            StableDeclarationKey::presentation,
        )
    }
}
