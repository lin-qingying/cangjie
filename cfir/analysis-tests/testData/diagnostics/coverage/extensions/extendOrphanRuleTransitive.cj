// RUN_PIPELINE_TILL: FRONTEND
// FILE: otherpackage.cj
package other

    public open class Foreign {}
    public interface ForeignInterface {}
    public interface ForeignChild <: ForeignInterface {}

// FILE: main.cj
package main

import other.*

interface LocalInterface <: ForeignChild {}

// 目标是外部类，但接口的传递闭包中有本地接口 → 合法
extend Foreign <: LocalInterface {}    //imported type 'Foreign' cannot extend imported interface, used external interface: Interface-ForeignChild Interface-ForeignInterface

// 目标是外部类，接口完全外部 → orphan rule 违规
extend <!EXTEND_ORPHAN_RULE!>Foreign<!> <: ForeignInterface {}  //imported type 'Foreign' cannot extend imported interface, used external interface: Interface-ForeignInterface
