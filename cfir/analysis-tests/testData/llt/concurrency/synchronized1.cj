// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import std.sync.*

var count: Int64 = 0

class Test
    <!parse_expected_lt_brace!>public<!> var mtx: ReentrantMutex<!parse_var_must_be_initialized!>
<!>    public <!parse_unexpected_declaration_in_scope!>init<!>() {
        this.mtx = ReentrantMutex()
    }

    public func testfunc(): Unit {
        synchronized(mtx) {
            count = count + 1
        }
    }
<!parse_expected_decl!>}<!>

main(): Int64 {
    return 0
}
