// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {}

class B <: A {}

class C <: A {}

main(): Int64 {
    let a = A()
    var b = B()
    var c = C()
    var res: B = match {
        case false => <!TYPE_MISMATCH!>a<!>
        case _ => <!TYPE_MISMATCH!>c<!>
    }
    return 0
}
