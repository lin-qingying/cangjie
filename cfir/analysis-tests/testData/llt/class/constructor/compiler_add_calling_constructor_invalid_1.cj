// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class C1 {
    private init() {
    }
}

class C2 <: C1 {
    public init() {
    }
}

main() {
    return 0
}

/* SCAN
error: there is no non-parameter constructor in super class, please invoke super call explicitly
  ==> compiler_add_calling_constructor_invalid_1.cj:15:12:
   | 
15 |     public init() {
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/
