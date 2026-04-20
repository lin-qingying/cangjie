package org.cangnova.cangjie.analysis.api.platform

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * An **engine service** as defined by the Platform Interface (see the README).
 *
 * Engine services do not need to be implemented by a platform. Quite the contrary, they are implemented by the Analysis API engine and
 * intended to support platform implementations. They are defined in the Platform Interface, as opposed to the user-facing Analysis API,
 * because they are intended for the consumption of platform implementations, but not Analysis API users.
 *
 * As an example, a platform's lifetime token implementation can make use of engine services to retrieve engine-managed state while keeping
 * the platform boundary explicit.
 *
 * As a marker interface, [CaEngineService] clearly separates an engine service from [CaPlatformComponent]s which need to be implemented
 * by a platform.
 */
@CaPlatformInterface
interface CaEngineService
