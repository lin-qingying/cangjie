package org.cangnova.cangjie

import com.intellij.openapi.project.Project
import com.intellij.pom.PomModelAspect
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.event.PomModelEvent
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.impl.source.PostprocessReformattingAspectImpl

/**
 * headless 的可修改 PSI 模型，按 SDK 253 LangPomModel 的完整职责连接标准后处理 aspect。
 * 平台的嵌套 LangPomModel 是包私有类型；这里使用公开基础类和公开 aspect，
 * 保留父模型的所有更新，并将同一事件交给真实后处理实现，不绕开格式化事务。
 */
internal class CangJieHeadlessPomModel(project: Project) : PomModelImpl(project) {
    private val postprocessAspect = PostprocessReformattingAspectImpl(project)

    override fun <T : PomModelAspect> getModelAspect(aspectClass: Class<T>): T? =
        if (aspectClass == PostprocessReformattingAspect::class.java ||
            aspectClass == PostprocessReformattingAspectImpl::class.java
        ) aspectClass.cast(postprocessAspect) else super.getModelAspect(aspectClass)

    protected override fun updateDependentAspects(event: PomModelEvent) {
        super.updateDependentAspects(event)
        postprocessAspect.update(event)
    }
}
