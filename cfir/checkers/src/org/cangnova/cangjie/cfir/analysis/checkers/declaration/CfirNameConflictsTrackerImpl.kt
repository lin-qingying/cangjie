package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * CFIR 名称冲突追踪器的默认内存实现。
 *
 * 该实现记录 classifier 维度的重声明信息，供后续冲突检查阶段按 [ClassId] 查询同名声明集合。
 */
class CfirNameConflictsTrackerImpl : CfirNameConflictsTracker() {
    /**
     * 单个 classifier 重声明条目。
     *
     * @property classifierSymbol 发生重声明的 class-like 符号。
     * @property containingFile 符号所在文件，外部或恢复场景下可能为空。
     */
    private data class ClassifierRedeclarationImpl(
        /**
         * 发生重声明的 class-like 符号。
         */
        override val classifierSymbol: CfirClassLikeSymbol<*>,

        /**
         * 符号所在文件，外部或恢复场景下可能为空。
         */
        override val containingFile: CfirFile?,
    ) : ClassifierRedeclaration()

    /**
     * 按 class id 分组的 classifier 重声明集合。
     */
    private val redeclaredClassifiers: MutableMap<ClassId, Set<ClassifierRedeclarationImpl>> = hashMapOf()

    /**
     * 查询指定 class id 对应的 classifier 重声明条目。
     */
    override fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration> =
        redeclaredClassifiers[classId].orEmpty()

    /**
     * 注册一对发生冲突的新旧 classifier 符号。
     *
     * 同一个 [classId] 多次注册时会合并集合，保留所有相关文件信息供诊断定位。
     */
    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    ) {
        redeclaredClassifiers.merge(
            classId,
            linkedSetOf(
                ClassifierRedeclarationImpl(newSymbol, newSymbolFile),
                ClassifierRedeclarationImpl(prevSymbol, prevSymbolFile),
            ),
        ) { first, second -> first + second }
    }
}
