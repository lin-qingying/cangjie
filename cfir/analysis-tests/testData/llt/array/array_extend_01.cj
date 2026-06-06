// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

class A{}
extend<<!parse_expected_name!>Int32<!>> Array<Int32> {  // 'Int32' here is treated as generic parameter which is invalid.
    func plus(a: Int32): Int32 { 0 }
}

extend<A> Array<A> {
    func plus(a: Int32): Int32 { 0 }
}

main(): Int32 {
    return 0
}
