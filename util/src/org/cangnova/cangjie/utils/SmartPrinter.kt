/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.utils

class SmartPrinter private constructor(private val printer: Printer) : IndentingPrinter by printer {
    constructor(appendable: Appendable, indent: String = DEFAULT_INDENT) : this(Printer(appendable, indent))

    companion object {
        private const val DEFAULT_INDENT = "    "
    }

    private var notFirstPrint: Boolean = false

    override fun print(vararg objects: Any?): SmartPrinter {
        if (notFirstPrint) {
            printer.printWithNoIndent(*objects)
        } else {
            printer.print(*objects)
        }
        notFirstPrint = true
        return this
    }

    override fun println(vararg objects: Any?): SmartPrinter {
        if (notFirstPrint) {
            printer.printlnWithNoIndent(*objects)
        } else {
            printer.println(*objects)
        }
        notFirstPrint = false
        return this
    }

    override fun printlnMultiLine(s: String): SmartPrinter {
        printer.printlnMultiLine(s)
        notFirstPrint = false
        return this
    }
}
