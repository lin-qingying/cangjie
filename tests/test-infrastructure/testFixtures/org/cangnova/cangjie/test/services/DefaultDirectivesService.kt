package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.directives.model.Directive

class DefaultDirectivesService(
    val directives: Set<Directive>,
) : TestService

val TestServices.defaultDirectives: DefaultDirectivesService by TestServices.testServiceAccessor()
