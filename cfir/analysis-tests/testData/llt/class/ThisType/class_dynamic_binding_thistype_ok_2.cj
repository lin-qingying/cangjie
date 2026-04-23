// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var x: Int64 = 0

open class C1 {
    public func foo1(): This {
        x = 1
        return this
    }
}

class C2 <: C1 {
    public func foo2(): This {
        x = 2
        return this
    }
}

main() {
    let obj = C2()
    obj.foo1().foo2()
    if (x == 2) {
        return 0
    }
    return 1
}
