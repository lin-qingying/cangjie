// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

enum A{
    a(Int64,Int64,Int64)
    public prop <!parse_expected_name!>:<!> Int64{
       get(v){1}
       set(v){ }
    }
}

main(): Int64 {
    var i = A.a(1,2,3)
    return i.a - 1
}
