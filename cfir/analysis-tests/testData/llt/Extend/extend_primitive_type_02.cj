// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

extend Int64 {
    public func add() {
        return this + 1
    }
}

main() {
    1.add
    1.addd
    0
}

/* SCAN
error: undeclared identifier 'addd'
  ==> extend_primitive_type_02.cj:17:7:
   | 
17 |     1.addd
   |       ^^^^ 
   | 

1 error generated, 1 error printed.
*/
