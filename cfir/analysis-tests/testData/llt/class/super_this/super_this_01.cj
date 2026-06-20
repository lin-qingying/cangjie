// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


abstract class FaTest {
    public var a: Int32 = 1
    public func f(): Int32
}

class Test1 <: FaTest {
    public init(x: Int32) {
        a = <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>s<!>uper.f()
    }
    public func f(): Int32 {
        return 5
    }
}

main() {
    return 0
}
