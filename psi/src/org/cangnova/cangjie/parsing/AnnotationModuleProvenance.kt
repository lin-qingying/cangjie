package org.cangnova.cangjie.parsing

import com.intellij.openapi.util.Key

/** 原输入注解的 parser 模块按确证 token 位置提供，新生成 token 使用本次 parser 模块。 */
val ANNOTATION_MODULE_PROVENANCE: Key<(Int) -> String?> = Key.create("cangjie.annotation.module.provenance")
