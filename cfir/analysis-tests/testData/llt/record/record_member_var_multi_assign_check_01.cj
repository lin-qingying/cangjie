// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct Rectangle {
    public let width: Int64
    public let length: Int64

    public init() {
        this.width = 10
        this.width = 11
        this.length = 20
        this.length = 21
    }
}

main(): Int64 {
    var rec = Rectangle()
    if (rec.width == 11 && rec.length == 21) {
        return 0
    }

    return 1
}

/* SCAN
error: cannot assign to immutable value
  ==> record_member_var_multi_assign_check_01.cj:15:9:
   | 
15 |         this.width = 11
   |         ^^^^^^^^^^^^^^^ 
   | 
note: variable 'width' is immutable
  ==> record_member_var_multi_assign_check_01.cj:10:16:
   | 
10 |     public let width: Int64
   |                ^^^^^ 
   | 

error: cannot assign to immutable value
  ==> record_member_var_multi_assign_check_01.cj:17:9:
   | 
17 |         this.length = 21
   |         ^^^^^^^^^^^^^^^^ 
   | 
note: variable 'length' is immutable
  ==> record_member_var_multi_assign_check_01.cj:11:16:
   | 
11 |     public let length: Int64
   |                ^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
