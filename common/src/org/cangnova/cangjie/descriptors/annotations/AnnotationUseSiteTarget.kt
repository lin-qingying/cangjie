/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.descriptors.annotations

import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly

/**
 * 对齐 Kotlin `AnnotationUseSiteTarget`。
 *
 * deprecation provider 需要与 accessor/property use-site 共享同一枚举契约，
 * 不能在 low-level 层私造另一套 target 类型。
 */
enum class AnnotationUseSiteTarget(renderName: String? = null) {
    ALL,
    FIELD,
    FILE,
    PROPERTY,
    PROPERTY_GETTER("get"),
    PROPERTY_SETTER("set"),
    RECEIVER,
    CONSTRUCTOR_PARAMETER("param"),
    SETTER_PARAMETER("setparam"),
    PROPERTY_DELEGATE_FIELD("delegate"),
    ;

    /**
     * 该 use-site target 在源码或诊断消息中的展示名称。
     */
    val renderName: String = renderName ?: name.toLowerCaseAsciiOnly()
}
