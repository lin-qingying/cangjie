package org.cangnova.cangjie.cli.common.arguments

data class CommonCompilerArguments(
    var languageVersion: String? = null,
    var languageFeatures: List<String> = emptyList(),
)
