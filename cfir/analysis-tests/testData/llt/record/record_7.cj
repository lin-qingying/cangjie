// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct Base12 {
    public var x: Int64 = 1
    public static prop a: Int64 {
        get() {
            return <!sema_static_function_cannot_access_non_static_member!>x<!>
        }
    }
}

main() {
    var x = Base12()
    return 0
}
