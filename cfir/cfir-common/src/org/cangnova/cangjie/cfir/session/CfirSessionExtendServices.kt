package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService

/**
 * 从 session 中读取必需的 extend 规则查询服务。
 */
val CfirSession.extendRuleQueryService: CfirExtendRuleQueryService by CfirSession.sessionComponentAccessor()

/**
 * 从 session 中读取可为空的 extend 规则查询服务。
 *
 * 早期构造阶段或不启用 extend 语义的 session 可以通过该访问器探测服务是否存在。
 */
val CfirSession.extendRuleQueryServiceOrNull: CfirExtendRuleQueryService? by CfirSession.nullableSessionComponentAccessor()
