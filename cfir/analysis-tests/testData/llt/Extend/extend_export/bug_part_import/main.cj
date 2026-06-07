// FILE: testa.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public interface IChild {
    func some(): Int64
}

public open class A {}

// FILE: testb.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a.*

public interface I2 {
    func some(): Int64
}

extend A <: I2 {
    public func some() {
        0
    }
}

public interface I1 {}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import a.*
import <!UNUSED_IMPORT!>b.I1<!>

public class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> <: A & IChild {}

main() {}
