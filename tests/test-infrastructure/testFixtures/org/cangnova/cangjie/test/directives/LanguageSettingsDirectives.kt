package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

object LanguageSettingsDirectives : SimpleDirectivesContainer() {
    val LANGUAGE_VERSION by stringDirective("Pin test language version.")
    val LANGUAGE by stringDirective("Enable/disable features, e.g. +Feature / -Feature.")
    val SUPPRESS_WARNINGS by stringDirective("Suppress warnings by diagnostic name.")
    val ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING by directive("Allow fixed language version in tests.")
}
