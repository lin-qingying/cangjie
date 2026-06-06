

package org.jetbrains.kotlin.generators

internal fun Array<String>.allowGenerationOnTeamCity(): Boolean {
    return any { it == "allowGenerationOnTeamCity" }
}

internal fun Array<String>.skipTestAllFilesCheck(): Boolean {
    return any { it == "skipTestAllFilesCheck" }
}
