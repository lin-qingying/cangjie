package org.cangnova.cangjie.analysis.api.annotations

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.ClassId

/**
 * 注解列表的视图。
 *
 * - 继承 `List<CaAnnotation>` 暴露按声明顺序遍历的能力;
 * - 额外提供按 [ClassId] 的存在性检查和分组读取,
 *   方便 IDE/工具层在不全量遍历的情况下定位特定注解。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationList`。
 */
interface CaAnnotationList : List<CaAnnotation>, CaLifetimeOwner {
    /** 当前列表中是否至少存在一个 [classId] 指定类型的注解。 */
    operator fun contains(classId: ClassId): Boolean

    /** 返回 [classId] 指定类型的全部注解,按声明顺序排列。 */
    operator fun get(classId: ClassId): List<CaAnnotation>

    /** 当前列表中出现过的注解类 ClassId 集合。 */
    val classIds: Collection<ClassId>
}
