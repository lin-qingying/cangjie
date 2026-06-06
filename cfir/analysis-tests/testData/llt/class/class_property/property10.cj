// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    return 0
}

open class Boo {
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public mut <!NOTHING_TO_OVERRIDE!>override<!> prop a6: Int64 {
    }<!>
}

class Coo <: Boo {}
