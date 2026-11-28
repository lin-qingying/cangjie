package org.cangnova.cangjie.cfir.source

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/** 库 SOURCE 的正式读取能力。返回完成 canonical 图；不授予 CFIR DFA/CFG 就绪能力。 */
interface CfirImportedSourceGraphProvider : CfirSessionComponent {
    /** 未保存正文或不属于此 provider 的真实声明返回 null；选中正文的材料化失败必须传播。 */
    fun sourceGraphOf(function: CfirFunction): CfirSourceGraph?
    /** 真实声明所属包的完成 SOURCE 产品；initializer/annotation/header-only 包不依赖非零函数正文。 */
    fun packageSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph?
    /** 原生header与signature/body可用性分离；查询此产品不会读取正文。 */
    fun sourceHeaderOf(function: CfirCallableDeclaration): CfirSourceImportedFunctionHeader?
    /** 显式完成原声明的default/annotation header内容；不授正文或initializer读取能力。 */
    fun sourceHeaderContentsOf(declaration: CfirDeclaration): Boolean
    /** 显式读取原F的owning槽证明以便随后查询local header，不物化正文或预绑定引用目标。 */
    fun discoverSourceFunctionHeadersOf(function: CfirFunction): Boolean
    /** 显式读取原 F 的普通正文并联合发布；复用 header/参数/目标，不授予 CFG 就绪能力。 */
    fun sourceBodyOf(function: CfirFunctionBodyOwner): CfirBlock?
    /** 显式读取原变量拥有的 initializer；header-first 与 content-first 使用同一个声明。 */
    fun sourceInitializerOf(variable: CfirVariable): CfirExpression?
}

val CfirSession.importedSourceGraphProvider: CfirImportedSourceGraphProvider? by CfirSession.nullableSessionComponentAccessor()
