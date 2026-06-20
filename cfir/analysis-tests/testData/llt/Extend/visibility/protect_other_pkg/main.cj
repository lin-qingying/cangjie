// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class C {}

extend C {
    protected func f() {
        1
    }
    protected mut prop p: Int64 {
        get() {
            0
        }
        set(v) {}
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

main() {
    let a = C()
    a.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>f<!>()
    <!INVALID_ACCESS_CONTROL!>a<!>.p = 0
    <!INVALID_ACCESS_CONTROL!>a<!>.p
    return 0
}
