package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

interface MacroFileRebuilder {
    fun rebuild(originalFile: CfirFile, updatedText: String): CfirFile
}
