// FILE: pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

public struct A {
    private var val = 1
    public mut prop a: Int64 {
        get() {
            return val
        }
        set(v) {
            val = v
        }
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import b.*

main() {
    var a = A()
    var res = a.a // 1
    a.a = 2
    res += a.a
    if (res != 3) {
        return 1
    }
    return 0
}
