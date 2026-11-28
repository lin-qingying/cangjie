package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * 统一函数返回目标。经真实 F-owner 的 symbol 取得声明，具名函数、lambda 和 enum F
 * 保持各自具体类型；raw 与保存正文共用同一个 AbstractTarget 绑定协议。
 */
class CfirFunctionTarget(
    labelName: String? = null,
    /** lambda 的真实角色；enum constructor F 始终为 false。 */
    val isLambda: Boolean,
) : CfirAbstractTarget<CfirFunctionBodyOwner>(labelName) {
    private lateinit var targetSymbol: CfirBasedSymbol<*>

    override val isBound: Boolean
        get() = this::targetSymbol.isInitialized

    override var _labeledElement: CfirFunctionBodyOwner
        get() {
            val declaration = targetSymbol.cfir
            check(declaration is CfirFunctionBodyOwner) {
                "A Return target must retain its actual function-body owner"
            }
            return declaration
        }
        set(value) {
            require(value.symbol.isBound && value.symbol.cfir === value) {
                "A Return target requires its actual bound F symbol"
            }
            require(isLambda == (value is CfirAnonymousFunction && value.isLambda)) {
                "A Return target must retain its actual lambda role"
            }
            // 与 Kotlin AbstractTarget.bind 和原生借用引用重定位一致：活动树允许
            // 重绑，已发布 SOURCE payload 的不变性由同代捕获及变更校验负责。
            targetSymbol = value.symbol
        }
}
