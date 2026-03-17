package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.Assertions

abstract class AssertionsService : Assertions(), TestService

val TestServices.assertions: AssertionsService by TestServices.testServiceAccessor()
