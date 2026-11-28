package org.cangnova.cangjie.cfir.resolve.providers.macro

/**
 * 已准备的非 IR 来源索引换代。调用方在所属 session 的同一写事务中验证所有产品，
 * 再共同提交；源槽位、metadata 和使用事实不能分别成为可见的新代次。
 */
interface CfirSourceGenerationUpdate {
    fun validate()
    fun commit()
}
