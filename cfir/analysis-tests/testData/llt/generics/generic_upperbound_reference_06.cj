// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I0 {
    func test() {}
}

interface I1 {
    prop test: Int64 {
        get() {
            1
        }
    }
}

class <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>A<!> <: I0 & I1 {}

func test<T>(a: T): Unit where T <: I0 & I1 {
    <!AMBIGUOUS_USE!>a.test<!>
}

main() {}
