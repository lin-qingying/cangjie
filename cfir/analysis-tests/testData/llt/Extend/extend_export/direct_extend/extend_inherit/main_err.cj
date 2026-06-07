// FILE: p1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

public open class A {}

extend A {
    protected func foo(): Int64 {
        1
    }
}

// FILE: p2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p2

import p1.A

public open class B <: A {}

// FILE: main_err.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import p2.B

class C <: B {
    func foo2() {
        <!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
    }
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>() {
        2
    }
}

main() {
    0
}
