package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 编译器扩展生成声明的基础 symbol provider。
 *
 * 具体实现注册到 session 后，[createIfNeeded] 可统一创建 wrapper，例如可开关的生成声明 provider。
 */
abstract class CfirExtensionDeclarationsSymbolProvider(
    session: CfirSession,
) : CfirSymbolProvider(session) {
    /**
     * 扩展声明 provider 工厂。
     */
    companion object {
        /**
         * 若当前 session 已注册扩展声明 provider，则返回该 provider。
         */
        fun createIfNeeded(session: CfirSession): CfirExtensionDeclarationsSymbolProvider? =
            session.extensionDeclarationsSymbolProviderOrNull
    }
}

/**
 * 当前 session 中可选注册的扩展声明 provider。
 */
private val CfirSession.extensionDeclarationsSymbolProviderOrNull: CfirExtensionDeclarationsSymbolProvider?
    by CfirSession.nullableSessionComponentAccessor()

/**
 * 将扩展生成声明 provider 注册到当前 session。
 */
fun CfirSession.registerExtensionDeclarationsSymbolProvider(provider: CfirExtensionDeclarationsSymbolProvider) {
    register(CfirExtensionDeclarationsSymbolProvider::class, provider)
}
