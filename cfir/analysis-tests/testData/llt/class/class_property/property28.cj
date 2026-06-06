// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    if (String.n != 1) {
        return 1
    }
    return 0
}

extend String {
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public <!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> static prop n: Int32 {
    }<!>
}
