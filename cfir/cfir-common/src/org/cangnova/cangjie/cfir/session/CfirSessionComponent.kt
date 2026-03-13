package org.cangnova.cangjie.cfir.session

/**
 * 标记接口，所有 session 组件的基接口。
 *
 * 每个编译器服务（provider、scope、diagnostic 等）都实现此接口，
 * 并通过 [CfirSession] 注册和查询。
 */
interface CfirSessionComponent
