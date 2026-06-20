// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    func f(): Unit
}

open class C1 {}

class C2 <: C1 {}

class A<T> {}

extend<T> A<T> where T <: C2 {
    public func f() {}
}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend<T> A<T> <: I where T <: C1 {}
