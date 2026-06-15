// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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

<!INHERIT_MEMBER_TYPE_INCONSISTENT!>e<!>xtend C1 <: I1 & I2 {
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>p<!>ublic prop foo: Int64 {
        get() {
            return 2
        }
    }
}

main() {
    return -1
}
