package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.Name

/**
 * Body resolve 上下文，负责管理 scope 栈、当前文件和声明容器。
 * 这里还统一持有返回类型计算器与数据流分析上下文。
 */
class CfirBodyResolveContext(
    var returnTypeCalculator: CfirReturnTypeCalculator,
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    private val isContextCollectorMode: Boolean = false,
) {

    lateinit var file: CfirFile

    var towerDataContext: CfirTowerDataContext = CfirTowerDataContext()
        private set

    // 保留当前最内层局部 scope 的独立引用，避免 copy-on-write 更新后丢失写入目标
    private var currentLocalScope: CfirLocalScopeImpl? = null

    val containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    fun addNonLocalScope(scope: CfirScope) {
        towerDataContext = towerDataContext.addNonLocalScope(scope)
    }

    fun addNonLocalScopes(scopes: List<CfirScope>) {
        towerDataContext = towerDataContext.addNonLocalScopes(scopes)
    }

    fun addLocalScope(localScope: CfirLocalScopeImpl) {
        towerDataContext = towerDataContext.addLocalScope(localScope)
        currentLocalScope = localScope
    }

    fun storeVariable(name: Name, symbol: CfirCallableSymbol<*>) {
        val localScope = currentLocalScope ?: return
        localScope.addVariable(name, symbol)
    }

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

    fun <T> withContainer(declaration: CfirDeclaration, action: () -> T): T {
        containers.addLast(declaration)
        return try {
            action()
        } finally {
            containers.removeLast()
        }
    }

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

