package org.cangnova.cangjie.test.services

class SourceFileProviderImpl(
    @Suppress("UNUSED_PARAMETER") testServices: TestServices,
    preprocessors: List<SourceFilePreprocessor>,
) : SourceFileProvider(preprocessors)
