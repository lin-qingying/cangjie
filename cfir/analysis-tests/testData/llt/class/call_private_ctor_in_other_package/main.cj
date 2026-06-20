// FILE: pkg/pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class Foo {
    init() {}
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

class Bar <: Foo {
    <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>i<!>nit() {
    }
    <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>i<!>nit(a: Int64) {
        let aNew = 1
    }
}
