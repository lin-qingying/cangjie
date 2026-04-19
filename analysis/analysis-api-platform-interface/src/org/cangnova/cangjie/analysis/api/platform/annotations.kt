/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.platform

/**
 * 被 [CaCachedService] 标注的属性缓存了 IntelliJ project/application service，
 * 或缓存了持有这类 service 引用的对象。
 *
 * 该注解用于标记热路径中的已缓存 service，帮助约束生命周期并提升可发现性。
 */
@Target(allowedTargets = [AnnotationTarget.PROPERTY, AnnotationTarget.FIELD])
annotation class CaCachedService
