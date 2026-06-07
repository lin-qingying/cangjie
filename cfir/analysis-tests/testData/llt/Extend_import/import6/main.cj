// FILE: A.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package A

public class Foo {}

// FILE: B.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package B

internal import A.Foo

public interface I1 {
    func f(): Unit
}

extend Foo <: I1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>(): Unit {
        print("I1.f\n")
    }
}

// FILE: C.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package C

internal import A.Foo

public interface I2 {
    func f(): Unit
}

extend Foo <: I2 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>(): Unit {
        print("I2.f\n")
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import A.Foo
import B.I1
import C.I2

main(): Int64 {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 1
}
