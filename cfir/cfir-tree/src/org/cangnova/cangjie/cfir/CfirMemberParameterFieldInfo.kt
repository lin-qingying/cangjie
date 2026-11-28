package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.cloneDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl

/**
 * 主构造成员参数新增的字段语法事实。
 * 参数本身不可变，字段的 var 与完整修饰符另行保存；名字、类型、注解和 owner 继续来自实际参数。
 * 内部状态快照不暴露可变引用，因此参数 attribute 的浅复制不会导致不同字段互相修改状态。
 */
class CfirMemberParameterFieldInfo(
    val isVar: Boolean,
    status: CfirDeclarationStatus,
) {
    private val capturedStatus: CfirDeclarationStatus = cloneDeclarationStatus(status)

    /** 每个实际字段取得自己的状态实例，不能改变参数描述中的原始事实。 */
    fun createFieldStatus(): CfirDeclarationStatusImpl = cloneDeclarationStatus(capturedStatus)
}

private object MemberParameterFieldInfoKey : CfirDeclarationDataKey()

private var CfirValueParameter.memberParameterFieldInfoAttribute: CfirMemberParameterFieldInfo? by
    CfirDeclarationDataRegistry.data(MemberParameterFieldInfoKey)

/** 源码主构造 LET/VAR 参数的字段描述；普通参数与已反序列化的字段不从这里补造事实。 */
val CfirValueParameter.memberParameterFieldInfo: CfirMemberParameterFieldInfo?
    get() = memberParameterFieldInfoAttribute

/** 在参数 raw 构建期间一次发布；调用方必须已确认真实 primary 参数身份及 LET/VAR 语法。 */
fun CfirValueParameter.recordMemberParameterFieldInfo(isVar: Boolean, status: CfirDeclarationStatus) {
    check(memberParameterFieldInfoAttribute == null) { "Member parameter field facts have already been recorded" }
    memberParameterFieldInfoAttribute = CfirMemberParameterFieldInfo(isVar, status)
    isMemberParameter = true
}
