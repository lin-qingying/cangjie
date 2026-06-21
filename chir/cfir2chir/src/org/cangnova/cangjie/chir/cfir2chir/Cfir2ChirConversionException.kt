package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.CfirElement

class Cfir2ChirConversionException(
    message: String,
    val element: CfirElement? = null,
) : IllegalStateException(message)
