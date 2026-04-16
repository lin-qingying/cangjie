package org.cangnova.cangjie.analysis.api.annotations

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.ClassId

interface CaAnnotationList : List<CaAnnotation>, CaLifetimeOwner {
    operator fun contains(classId: ClassId): Boolean

    operator fun get(classId: ClassId): List<CaAnnotation>

    val classIds: Collection<ClassId>
}
