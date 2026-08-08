// RUN_PIPELINE_TILL: FRONTEND
// FILE: otherpackage.cj
package other

    public open class Foreign {}
    public interface ForeignInterface {}
    public interface ForeignChild <: ForeignInterface {}

// FILE: main.cj
package localmain

import other.*

interface LocalInterface <: ForeignChild {}

// 目标是外部类，被使用到的接口闭包仍然完全来自外部包，仍然违反 orphan rule
extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>Foreign<!> <: LocalInterface {}    // imported type 'Foreign' cannot extend imported interface, used external interface: Interface-ForeignChild Interface-ForeignInterface

// 目标是外部类，接口完全外部 → orphan rule 违规
extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>Foreign<!> <: ForeignInterface {}  // imported type 'Foreign' cannot extend imported interface, used external interface: Interface-ForeignInterface
