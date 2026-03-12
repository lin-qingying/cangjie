/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.patterns.builder

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangjie.cfir.patterns.impl.CfirWildcardPatternImpl

@OptIn(CfirImplementationDetail::class)
fun buildWildcardPattern(): CfirWildcardPattern {
    return CfirWildcardPatternImpl()
}
