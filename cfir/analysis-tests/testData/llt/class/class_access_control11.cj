// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class R {
    private var x: Int64 = 2
    public func a() {
        var c: Int32 = 10
        return A()
    }
    private func b() {
        return R().x
    }
}

class A {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    R().a().<!INVISIBLE_MEMBER!>a<!>
    R().<!INVISIBLE_MEMBER!>b<!>

    return 0
}
