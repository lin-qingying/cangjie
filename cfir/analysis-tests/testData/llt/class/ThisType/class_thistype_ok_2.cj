// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C1 {
    public var x = 0
    public func foo1(): This {
        return foo2()
    }

    public func foo2(): This {
        return foo3()
    }

    public func foo3(): This {
        x = 1
        return this
    }
}

main() {
    var obj = C1()
    if (obj.foo1().x == 1) {
        return 0
    }
    return 1
}
