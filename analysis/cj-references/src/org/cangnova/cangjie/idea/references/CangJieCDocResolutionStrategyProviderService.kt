package org.cangnova.cangjie.idea.references

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

/**
 * CDoc 引用解析策略的项目级开关服务。
 *
 * IDE 或测试宿主可以通过该服务控制是否使用实验性 CDoc 解析策略。
 */
interface CangJieCDocResolutionStrategyProviderService : Disposable {
    /**
     * 返回当前项目是否启用实验性 CDoc 引用解析策略。
     */
    fun shouldUseExperimentalStrategy(): Boolean

    companion object {
        /**
         * 从项目服务容器中取得可选的 CDoc 解析策略服务。
         */
        fun getService(project: Project): CangJieCDocResolutionStrategyProviderService? =
            project.serviceOrNull()
    }
}
