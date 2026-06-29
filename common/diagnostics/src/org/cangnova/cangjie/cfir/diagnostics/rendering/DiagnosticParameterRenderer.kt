/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.cangnova.cangjie.cfir.diagnostics.rendering

/**
 * 诊断参数渲染器接口。
 */
interface DiagnosticParameterRenderer<in O> {
    /**
     * 在指定渲染上下文中渲染参数对象。
     */
    fun render(obj: O, renderingContext: RenderingContext): String
}

/**
 * 不依赖渲染上下文的诊断参数渲染器。
 */
interface ContextIndependentParameterRenderer<in O> : DiagnosticParameterRenderer<O> {
    /**
     * 忽略上下文并调用无上下文渲染方法。
     */
    override fun render(obj: O, renderingContext: RenderingContext): String = render(obj)

    /**
     * 渲染参数对象。
     */
    fun render(obj: O): String
}

/**
 * 根据 lambda 创建上下文无关参数渲染器。
 */
fun <O> Renderer(block: (O) -> String) = object : ContextIndependentParameterRenderer<O> {
    /**
     * 使用传入 lambda 渲染参数。
     */
    override fun render(obj: O): String = block(obj)
}

/**
 * 根据 lambda 创建依赖渲染上下文的参数渲染器。
 */
fun <O> ContextDependentRenderer(block: (O, RenderingContext) -> String) = object : DiagnosticParameterRenderer<O> {
    /**
     * 使用传入 lambda 和上下文渲染参数。
     */
    override fun render(obj: O, renderingContext: RenderingContext): String = block(obj, renderingContext)
}

/**
 * 使用指定渲染器渲染参数；未提供渲染器时返回参数本身。
 */
fun <P> renderParameter(parameter: P, renderer: DiagnosticParameterRenderer<P>?, context: RenderingContext): Any? =
    renderer?.render(parameter, context) ?: parameter
