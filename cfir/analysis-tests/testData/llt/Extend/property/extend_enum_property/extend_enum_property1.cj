// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    if (Data.n != 1) {
        return 1
    }
    Data.m = 10
    if (Data.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
    public mut prop m: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop n: Int64 {
        get() {
            return 1
        }
    }
}

/* SCAN
error: 'n' is non-static member, cannot access by type name
  ==> extend_enum_property1.cj:10:9:
   | 
10 |     if (Data.n != 1) {
   |         ^ 
   | 

error: 'm' is non-static member, cannot access by type name
  ==> extend_enum_property1.cj:13:5:
   | 
13 |     Data.m = 10
   |     ^ 
   | 

error: 'm' is non-static member, cannot access by type name
  ==> extend_enum_property1.cj:14:9:
   | 
14 |     if (Data.m != 2) {
   |         ^ 
   | 

error: there cannot have mutable property in immutable type
  ==> extend_enum_property1.cj:25:12:
   | 
25 |     public mut prop m: Int64 {
   |            ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
