package org.cangnova.cangjie.test.services

object FrontendBasedFacadesMarker : TestService

val frontendBasedFacadesMarkerRegistrationData: ServiceRegistrationData =
    service { _: TestServices -> FrontendBasedFacadesMarker }
