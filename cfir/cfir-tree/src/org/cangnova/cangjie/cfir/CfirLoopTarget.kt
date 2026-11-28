package org.cangnova.cangjie.cfir

/**
 * 统一循环目标，保存真实 while、do-while 或 For 的迭代 owner。
 * 目标不要求 condition，不沿借用边创建循环；复制时用新 target 绑定新 owner。
 */
class CfirLoopTarget(
    labelName: String? = null,
) : CfirAbstractTarget<CfirIterationOwner>(labelName) {
    private lateinit var loop: CfirIterationOwner

    override val isBound: Boolean
        get() = this::loop.isInitialized

    override var _labeledElement: CfirIterationOwner
        get() = loop
        set(value) {
            // 活动树的借用引用可被正式重定位；冻结产品保存独立 owner 身份。
            loop = value
        }
}
