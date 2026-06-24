package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * CFIR 名称冲突追踪服务。
 *
 * 该服务记录同一 [ClassId] 下的 classifier 重声明关系，供诊断收集阶段报告冲突。
 */
abstract class CfirNameConflictsTracker : CfirSessionComponent {
    /**
     * 单个 classifier 重声明记录。
     */
    abstract class ClassifierRedeclaration {
        /**
         * 发生重声明的 classifier 符号。
         */
        abstract val classifierSymbol: CfirClassLikeSymbol<*>

        /**
         * 该声明所在文件；库符号可能没有文件。
         */
        abstract val containingFile: CfirFile?
    }

    /**
     * 获取 [classId] 对应的所有 classifier 重声明记录。
     */
    abstract fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration>

    /**
     * 注册一个新的 classifier 重声明关系。
     */
    abstract fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    )
}

/**
 * 从 session 中读取可为空的名称冲突追踪服务。
 */
val CfirSession.nameConflictsTracker: CfirNameConflictsTracker? by CfirSession.nullableSessionComponentAccessor()
