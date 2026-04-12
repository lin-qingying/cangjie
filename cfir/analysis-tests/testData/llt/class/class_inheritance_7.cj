// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output


main(): Int64 {
    if ( C().a != 1 ){
        return 1
    }
    return 0
}

open class A<T> {
    public func foo(){
        return 1
    }
}

class C <: A<Int6

/* SCAN
error: expected '{', found '<EOF>'
  ==> class_inheritance_7.cj:23:18:
   | 
23 | class C <: A<Int6
   |                  ^ expected '{' here
   | 

error: unclosed delimiter: '<'
  ==> class_inheritance_7.cj:23:18:
   | 
23 | class C <: A<Int6
   |             ~    ^ expected '>' here
   |             |
   |             to match this opening '<'
   | 

2 errors generated, 2 errors printed.
*/
