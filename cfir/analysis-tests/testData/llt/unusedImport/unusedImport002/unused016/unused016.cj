// FILE: a.cj
package a 

public interface I {
    func testFunc() : Unit
}

public interface X {
}

extend<T> Array<T> <: I where T<: X {
    public func testFunc() : Unit{}
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: b.cj
package b

import a.X

public class A <: X {}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: unused016.cj
package c
import a.X
import a.I
import b.A

main() {
   var a = Array<A>()
   a.testFunc()
   return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
