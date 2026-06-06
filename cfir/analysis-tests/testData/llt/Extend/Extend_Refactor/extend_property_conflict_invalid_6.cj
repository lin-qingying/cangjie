// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int32 {
        get() {
            return 2
        }
    }
}

class C1 {}

<!sema_inherit_member_type_inconsistent!>e<!>xtend C1 <: I1 & I2 {
    <!sema_property_override_implement_type_diff!>p<!>ublic prop foo: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}
