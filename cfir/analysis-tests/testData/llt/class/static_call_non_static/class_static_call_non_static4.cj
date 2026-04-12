// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %compile_lib_opt %f 2>&1 | compare %f

class Base {
    public func f(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        func a(): Int32 {
            return f()
        }
        return a()
    }
}

/* SCAN
error: 'f' is non-static member, cannot be accessed by static function 'a'
  ==> class_static_call_non_static4.cj:15:20:
   | 
15 |             return f()
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
