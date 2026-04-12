// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main() : Int64 {
    return 0
}

type A = This

extend A {
    public operator func ==(right:Rune):Bool  {
        if (right == 'a') {
            return true
        }else{
            return false
        }
    }
}

/* SCAN
error: 'This' type is not allowed
  ==> extend_This_type3.cj:13:10:
   | 
13 | type A = This
   |          ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

1 error generated, 1 error printed.
*/
