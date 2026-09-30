package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.name.FqName

/**
 * 结构外 `.cjo` 反序列化使用的最小 session。
 *
 * 对位 Kotlin `FirLibrarySession` 在“按需读取库声明”上的角色，但只保留 `.cjo` 反序列化真正会读的组件：
 * - `CfirLanguageSettingsComponent`：`.cjo` 声明物化必须消费所属 session 的同一份语言设置
 *   （`CfirDeserializationContext.languageVersionSettings`），不能为二进制重新造默认设置；
 * - `CfirCangJieScopeProvider`：包成员 scope / 类成员 scope 反序列化经由它进入 `CfirDeclDeserializer`。
 *
 * 其余组件（`implicitSystemAnnotations`、`cjmpSettings`、`interopSettings`、`cfirAbiPolicy`）走
 * `sessionComponentAccessorWithDefault` 的默认实现，`cjmpLoadDiagnostics` 是可空组件。
 *
 * 刻意不注册 `CfirSymbolProvider`：反序列化路径在 `AbstractCfirDeserializedSymbolProvider` 中显式传入
 * provider，session 组件容器里不需要它；在半成品 session 上读 `symbolProvider` 会以
 * “No ... in array owner” 失败，而不是静默回落。
 */
class DetachedCjoSession(
    /** 结构外 `.cjo` 的包全限定名，仅用于诊断标识。 */
    val packageFqName: FqName,
    /** 库侧语言设置，取自项目结构的 `libraryLanguageVersionSettings`。 */
    languageVersionSettings: LanguageVersionSettings,
) : CfirSession(Kind.Library) {
    init {
        register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(languageVersionSettings))
        register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
    }
}
