// FILE: A.cj
package A
public class Foo { }


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: B.cj
package B
internal import A.Foo

public interface I1 {
    func f(): Unit {}
}

extend Foo <: I1 {}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: C.cj
package C
internal import A.Foo

public interface I2 {
    func f(): Unit {}
}

extend Foo <: I2 {}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: main.cj
internal import A.Foo
internal import B.I1
internal import C.I2

main(): Int64 {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 1
}
