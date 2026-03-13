package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * Body 解析上下文，管理 scope 塔和声明容器栈。
 *
 * 通过构造函数注入核心依赖：
 * - [returnTypeCalculator]：声明返回类型计算器
 * - [dataFlowAnalyzerContext]：数据流分析上下文
 *
 * 运行时状态：
 * - [towerDataContext]：scope 塔（copy-on-write 语义）
 * - [file]：当前处理的文件
 * - [containers]：声明容器栈（文件 → 类 → 函数 → 块）
 *
 * 参考 K2 BodyResolveContext(returnTypeCalculator, dataFlowAnalyzerContext, isContextCollectorMode)。
 */
class CfirBodyResolveContext(
    var returnTypeCalculator: CfirReturnTypeCalculator,
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    private val isContextCollectorMode: Boolean = false,
) {

    /** 当前文件 */
    lateinit var file: CfirFile

    /** 当前 scope 塔上下文 */
    var towerDataContext: CfirTowerDataContext = CfirTowerDataContext()
        private set

    /**
     * 当前最内层局部 scope 引用。
     *
     * 由于 [CfirTowerDataContext] 使用 copy-on-write 语义（返回新 List），
     * 通过 towerDataContext.localScopes 取得的对象在后续 towerDataContext 更新后
     * 不再是同一实例。因此维护一个独立引用，确保 [storeVariable] 能正确写入可变的局部 scope。
     */
    private var currentLocalScope: CfirLocalScopeImpl? = null

    /** 声明容器栈 */
    val containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    /** 当前最内层容器 */
    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    // ---- scope 管理（委托到 towerDataContext） ----

    /** 添加非局部 scope（包、导入、类成员等） */
    fun addNonLocalScope(scope: CfirScope) {
        towerDataContext = towerDataContext.addNonLocalScope(scope)
    }

    /** 批量添加非局部 scope */
    fun addNonLocalScopes(scopes: List<CfirScope>) {
        towerDataContext = towerDataContext.addNonLocalScopes(scopes)
    }

    /** 添加局部 scope（函数体/块内变量 scope） */
    fun addLocalScope(localScope: CfirLocalScopeImpl) {
        towerDataContext = towerDataContext.addLocalScope(localScope)
        currentLocalScope = localScope
    }

    /** 添加局部变量到当前最内层局部 scope */
    fun storeVariable(name: Name, symbol: CfirVariableSymbol) {
        val localScope = currentLocalScope ?: return
        localScope.addVariable(name, symbol)
    }

    // ---- 上下文切换（scoped 方法） ----

    /**
     * 在新的 scope 上下文中执行操作，执行完后恢复。
     *
     * 对齐 K2 BodyResolveContext 的 withTowerDataContext 模式。
     */
    fun <T> withTowerDataContext(newContext: CfirTowerDataContext, action: () -> T): T {
        val old = towerDataContext
        val oldLocalScope = currentLocalScope
        towerDataContext = newContext
        currentLocalScope = null
        return try {
            action()
        } finally {
            towerDataContext = old
            currentLocalScope = oldLocalScope
        }
    }

    /** 将声明压入容器栈，执行操作后弹出。 */
    fun <T> withContainer(declaration: CfirDeclaration, action: () -> T): T {
        containers.addLast(declaration)
        return try {
            action()
        } finally {
            containers.removeLast()
        }
    }

    /** 设置当前文件并执行操作。 */
    fun <T> withFile(file: CfirFile, action: () -> T): T {
        val oldFile = if (::file.isInitialized) this.file else null
        this.file = file
        return try {
            withContainer(file, action)
        } finally {
            if (oldFile != null) this.file = oldFile
        }
    }
}
