// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {
    func foo(a: T): T
}

class C2 <: I<Bool> & I<Int32> {
    public func foo(a: Int32): Int32 {
        return 0;
    }

    public func foo(a: Bool): Bool {
        return false;
    }
}

main(): Int64 {
    var a: C2 = C2();
    a.foo(1);
    a.foo(true);
    return 0;
}
