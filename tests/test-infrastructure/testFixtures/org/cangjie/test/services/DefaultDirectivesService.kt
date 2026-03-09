package org.cangjie.test.services

import org.cangjie.test.directives.model.Directive

class DefaultDirectivesService(
    val directives: Set<Directive>,
) : TestService

val TestServices.defaultDirectives: DefaultDirectivesService by TestServices.testServiceAccessor()
