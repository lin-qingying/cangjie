package org.cangnova.cangjie.test.services

object CliBasedFacadesMarker : TestService

val cliBasedFacadesMarkerRegistrationData: ServiceRegistrationData =
    service { _: TestServices -> CliBasedFacadesMarker }
