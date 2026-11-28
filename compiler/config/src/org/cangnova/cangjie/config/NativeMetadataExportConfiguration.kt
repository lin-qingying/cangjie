package org.cangnova.cangjie.config

/**
 * 原生 metadata 的 invocation 输入；产物是否为增量缓存由写出入口决定，不能由增量编译开关代替。
 * source 导入、包导出范围与 inline capability 均分别保留。
 */
data class NativeMetadataExportConfiguration(
    val importSourceCode: Boolean = true,
    val noSubPackages: Boolean = false,
    val exportForTest: Boolean = false,
    val nativeLlvm: Boolean = true,
    val hotReload: Boolean = false,
    val mockEnabled: Boolean = false,
) {
    /** SOURCE 的 CheckInlineFunctions 门；与读取已有 CJO 的 inline capability 分开。 */
    fun canInlineSource(enableCompileTest: Boolean): Boolean = nativeLlvm && !enableCompileTest && !hotReload && !mockEnabled

    /** v1.1.3 CjoManager::CanInline：读取端还要求优化级别高于 O1，不读取源码 mock 门。 */
    fun canInlineImported(enableCompileTest: Boolean, optimizationLevel: Int): Boolean =
        nativeLlvm && optimizationLevel > 1 && !enableCompileTest && !hotReload
}

private val NATIVE_METADATA_EXPORT_CONFIGURATION =
    CompilerConfigurationKey.create<NativeMetadataExportConfiguration>("native metadata export configuration")

var CompilerConfiguration.nativeMetadataExportConfiguration: NativeMetadataExportConfiguration
    get() = get(NATIVE_METADATA_EXPORT_CONFIGURATION, NativeMetadataExportConfiguration())
    set(value) {
        put(NATIVE_METADATA_EXPORT_CONFIGURATION, value)
    }
