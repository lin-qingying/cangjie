// FILE: pkg.cj
package pkg

import pkg.base.Base

public open class Derived <: Base {
    let value: Int64 = 1
    prop b: Int64{
        get() {
            value
        }
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: base.cj
package pkg.base

public open class Base {
    let value: Int64 = 0
    prop b: Int64{
        get() {
            value
        }
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: test.cj
package pkg.base.derived

import pkg.Derived

public class SubDerived <: Derived {
    public func print(): Unit {
        println(b)
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
