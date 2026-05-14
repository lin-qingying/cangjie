// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class Base {}

class Derived <: Base {}

interface I1 {
    func foo(): Base
}

abstract class C1 <: I1 {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> <: I1 {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> <: I1 {
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): Int64 {
        return 0
    }
}

class C4 <: <!UNRESOLVED_REFERENCE!>I2<!> {
    public func foo(): Base {
        return Base()
    }
}

class C5 <: <!UNRESOLVED_REFERENCE!>I2<!> {
    public func foo(): Derived {
        return Derived()
    }
}

main(): Int64 {
    return 0
}
