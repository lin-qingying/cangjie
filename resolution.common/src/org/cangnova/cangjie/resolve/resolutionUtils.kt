/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve

/**
 *  Shouldn't be visible to users.
 *  Used as prefix for [org.cangnova.cangjie.name.FqName] from non-root to avoid conflicts when resolving in IDE.
 *  E.g.:
 *  ---------
 *  package a
 *
 *  class A
 *
 *  fun test(a: Any) {
 *      a.A() // invalid code -> incorrect import/completion/etc.
 *      _root_ide_package_.a.A() // OK
 *  }
 *  ---------
 */
const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"
/**
 * IDE root prefix 的点号后缀形式，用于拼接全限定名。
 */
const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT = "$ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE."
