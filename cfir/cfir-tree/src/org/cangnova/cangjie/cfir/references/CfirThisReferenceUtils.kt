package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.references.builder.CfirThisReferenceBuilder

inline fun buildImplicitThisReference(init: CfirThisReferenceBuilder.() -> Unit): CfirThisReference =
    CfirThisReferenceBuilder().apply {
        isImplicit = true
        init()
    }.build()
