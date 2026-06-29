package org.cangnova.cangjie.test.services

/**
 * 表示 `SourceFileProviderImpl`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class SourceFileProviderImpl(
    @Suppress("UNUSED_PARAMETER") testServices: TestServices,
    preprocessors: List<SourceFilePreprocessor>,
) : SourceFileProvider(preprocessors)
