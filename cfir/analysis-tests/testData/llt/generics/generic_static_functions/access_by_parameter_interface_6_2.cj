// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


abstract class I {
    public static func foo() {
        return 0
    }
}

class D<T> where T <: I {
    public func foo() {
        return T.foo()
    }
}

main() {
    return D<I>().foo()
}
