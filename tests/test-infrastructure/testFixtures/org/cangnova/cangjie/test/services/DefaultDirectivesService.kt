package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
class DefaultRegisteredDirectivesProvider(defaultGlobalDirectives: RegisteredDirectives) : TestService {
    val defaultDirectives: RegisteredDirectives by lazy {
        defaultGlobalDirectives
    }
}
val TestServices.defaultRegisteredDirectivesProvider: DefaultRegisteredDirectivesProvider by TestServices.testServiceAccessor()

val TestServices.defaultDirectives: RegisteredDirectives
    get() = defaultRegisteredDirectivesProvider.defaultDirectives
