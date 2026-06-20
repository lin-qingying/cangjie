// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// if both two functions with the same signature have default implementations, they must be overridden in the extend decl
interface A {
    func add(): Unit {}
}

interface B {
    func add(): Unit {}
}

class S {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend S <: A & B {}
