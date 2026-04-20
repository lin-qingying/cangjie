package org.cangnova.cangjie.cfir.session

/**
 * CJMapping interop 配置提供方。
 *
 * 对齐 C++ `globalOptions.interopCJPackageConfigPath` (CompileStrategy.cpp:51):
 * 编译驱动读入 `--interop-cj-package-config` 选项,指向一个描述 CJMapping 泛型
 * 方法实例化规则的 JSON 文件。若路径设置但文件无法解析,报
 * `CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG`。
 *
 * - [configPath]:未配置时为 `null`,跳过检查。
 * - [isValid]:默认 true;驱动在加载 config 失败时设为 false 并通过 `configPath`
 *   携带原始路径,checker 根据此标志发诊断。
 */
interface CfirCJMappingConfigProvider : CfirSessionComponent {
    val configPath: String? get() = null
    val isValid: Boolean get() = true
}

object DefaultCfirCJMappingConfigProvider : CfirCJMappingConfigProvider

val CfirSession.cjMappingConfigProvider: CfirCJMappingConfigProvider by CfirSession.sessionComponentAccessorWithDefault(
    DefaultCfirCJMappingConfigProvider
)
