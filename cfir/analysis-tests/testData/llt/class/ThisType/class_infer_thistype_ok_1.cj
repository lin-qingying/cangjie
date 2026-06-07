// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1 {
    public func foo1() {
        return this
    }

    public func foo3() {
        try {
            return this
        } finally {
        }
    }

    public func foo4(): This {
        try {
            return this
        } finally {
        }
    }
}

class C2 <: C1 {
    public func foo2() {
        return this
    }
}

main() {
    let obj = C2()
    obj.foo1().foo2()
    obj.foo3().foo2()
    obj.foo4().foo2()
    return 0
}
