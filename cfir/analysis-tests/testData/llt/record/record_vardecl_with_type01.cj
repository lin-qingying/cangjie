// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct RA {
    public let a: Int64 = 10
    public init(a: Int64) {
        this.a = a
    }
}

main() {
    var ra = RA(5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}

/* SCAN
error: cannot assign to immutable value
  ==> record_vardecl_with_type01.cj:12:9:
   | 
12 |         this.a = a
   |         ^^^^^^^^^^ 
   | 
note: variable 'a' is immutable
  ==> record_vardecl_with_type01.cj:10:16:
   | 
10 |     public let a: Int64 = 10
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
