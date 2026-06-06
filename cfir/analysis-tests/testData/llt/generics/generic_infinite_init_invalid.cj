// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

// generic recursive checking for invalid type. was stackoverflow, should terminate normally

struct A<X,Y>{
    public operator func <=(b:A<Int32,(Int32,A<Int32,Int32<!parse_expected_right_delimiter!>)<!>>):Bool {true}
}

main() {}
