/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.decompiled.psi

import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile

/**
 * 对位 Kotlin `KotlinBuiltinsDecompiledFile`：
 * 该文件表示从 `.cjo` package binary 恢复出的反编译 PSI 文件。
 */
class CangJieBuiltinsDecompiledFile(viewProvider: CangJieDecompiledFileViewProvider) : CjDecompiledFile(viewProvider)
