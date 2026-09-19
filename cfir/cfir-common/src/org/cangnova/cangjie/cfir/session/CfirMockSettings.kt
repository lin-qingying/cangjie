package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.config.MockSupportKind

/**
 * 当前编译调用的 mock capability。
 *
 * 该组件是 checker、声明 provider 和后续 mock preparation consumer 的唯一读取入口。
 * 它只承载 session 已确认的配置事实；任何 consumer 都不得通过文件名或源码路径推断
 * test/mock 模式。
 */
public open class CfirMockSettingsComponent(
    /** 对应官方 `GlobalOptions.enableCompileTest`。 */
    public val enableCompileTest: Boolean = false,
    /** 对应官方 `GlobalOptions.mock`。 */
    public val mockSupportKind: MockSupportKind = MockSupportKind.DEFAULT,
) : CfirSessionComponent {
    /** DEFAULT 只有在 test compilation 中才建立按需 mock 兼容能力。 */
    public val mockCompatibleIfNeeded: Boolean
        get() = enableCompileTest && mockSupportKind == MockSupportKind.DEFAULT

    /** 显式 `--mock=on` 建立 mock 兼容能力。 */
    public val explicitMockCompatible: Boolean
        get() = mockSupportKind == MockSupportKind.ON

    /** 当前 package 是否可进入 mock preparation 路径。 */
    public val mockCompatible: Boolean
        get() = mockCompatibleIfNeeded || explicitMockCompatible

    /** `runtime-error` 允许编译但禁止真正创建 mock。 */
    public val mockCompileOnly: Boolean
        get() = mockSupportKind == MockSupportKind.RUNTIME_ERROR
}

/** 未配置 mock capability 时使用的严格默认值。 */
public object DefaultCfirMockSettingsComponent : CfirMockSettingsComponent()

/** 当前 session 的 mock capability。 */
public val CfirSession.mockSettings: CfirMockSettingsComponent by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirMockSettingsComponent)
