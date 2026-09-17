/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.CjFeaturesDirective
import org.cangnova.cangjie.psi.stubs.CangJieFeaturesDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/** `features` directive 的可序列化 stub 实现。 */
class CangJieFeaturesDirectiveStubImpl(
    parent: StubElement<*>?,
    override val featureIds: List<String>,
) : CangJieStubBaseImpl<CjFeaturesDirective>(parent, CjStubElementTypes.FEATURES_DIRECTIVE),
    CangJieFeaturesDirectiveStub {
    override fun copyInto(newParent: StubElement<*>?): CangJieFeaturesDirectiveStubImpl =
        CangJieFeaturesDirectiveStubImpl(newParent, featureIds)
}
