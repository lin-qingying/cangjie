// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func i(a: T): Bool
}

open class Base<U> {
    public open func base(): Int64 {
        return 0
    }
}

class A <: Base<Bool> & I<Bool> {
    public func i(a: Bool): Bool {
        return false
    }
    public override func base(): Int64 {
        return 1
    }
}

main() {
    var obj: A = A()
    if (obj.i(true) == false && obj.base() == 1) {
        return 0
    }
    return 1
}
