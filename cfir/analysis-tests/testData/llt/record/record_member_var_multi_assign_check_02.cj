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
        this.length = 20
    }

    public init(width: Int64, length: Int64) {
        this()
        this.width = width
        this.length = length
    }
}

main(): Int64 {
    var rec = Rectangle(100, 200)
    if (rec.width == 100 && rec.length == 200) {
        return 0
    }
    return 1
}

/* SCAN
error: cannot assign to immutable value
  ==> record_member_var_multi_assign_check_02.cj:20:9:
   | 
20 |         this.width = width
   |         ^^^^^^^^^^^^^^^^^^ 
   | 
note: variable 'width' is immutable
  ==> record_member_var_multi_assign_check_02.cj:10:16:
   | 
10 |     public let width: Int64
   |                ^^^^^ 
   | 

error: cannot assign to immutable value
  ==> record_member_var_multi_assign_check_02.cj:21:9:
   | 
21 |         this.length = length
   |         ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: variable 'length' is immutable
  ==> record_member_var_multi_assign_check_02.cj:11:16:
   | 
11 |     public let length: Int64
   |                ^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
