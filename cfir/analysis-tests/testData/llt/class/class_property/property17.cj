// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main():Int64 {
    return 0
}

open class Boo{
    public open prop <!parse_expected_name!>func<!> foo(){}
}

class Coo <: Boo{
    public override prop <!parse_expected_name!>func<!> foo(){}
}
