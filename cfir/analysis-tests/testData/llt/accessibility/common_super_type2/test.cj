// FILE: a.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pa

import std.collection.*
import <!UNUSED_IMPORT!>std.unittest<!>
import <!UNUSED_IMPORT!>std.unittest.testmacro<!>

func foo() {
    return ["", ArrayList<Any>()]
}

// FILE: test.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package pa

private import std.ast.*

main() {
    let arr: Array<ToTokens> = <!TYPE_MISMATCH!>foo()<!>
}
