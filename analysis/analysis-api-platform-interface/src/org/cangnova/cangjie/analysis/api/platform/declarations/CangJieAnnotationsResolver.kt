package org.cangnova.cangjie.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjAnnotated

/**
 * `CangJieAnnotationsResolver` 对位 Kotlin `KotlinAnnotationsResolver`。
 *
 * 该服务负责在“声明 <-> 注解类型”之间做近似映射。
 * 由于平台实现通常不能在索引层执行完整解析，因此结果允许出现 false positive / false negative。
 */
@CaPlatformInterface
interface CangJieAnnotationsResolver {
    /**
     * 返回带有 [annotationClassId] 注解的近似声明集合。
     */
    fun declarationsByAnnotation(annotationClassId: ClassId): Set<CjAnnotated>

    /**
     * 返回声明上出现过的近似注解类型集合。
     */
    fun annotationsOnDeclaration(declaration: CjAnnotated): Set<ClassId>
}

/**
 * `CangJieAnnotationsResolver` 的平台工厂。
 */
@CaPlatformInterface
interface CangJieAnnotationsResolverFactory : CaPlatformComponent {
    /**
     * 创建只在 [searchScope] 内工作的注解解析器。
     */
    fun createAnnotationResolver(searchScope: GlobalSearchScope): CangJieAnnotationsResolver

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级注解解析器工厂服务。
         */
        fun getInstance(project: Project): CangJieAnnotationsResolverFactory = project.service()
    }
}

/**
 * 使用项目平台注册的工厂创建注解解析器。
 */
@CaPlatformInterface
fun Project.createAnnotationResolver(searchScope: GlobalSearchScope): CangJieAnnotationsResolver =
    CangJieAnnotationsResolverFactory.getInstance(this).createAnnotationResolver(searchScope)
