// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was segv, should quit normally
interface A{
    prop@b: Int64
}

struct B <: A{
    public prop b: Int64{
        get(){
            0
        }
    }
}

main():Int64 {
    let a = B()
    return a.b
}

/* SCAN
error: expected a property name after keyword 'prop' in property definition, found '@'
 ==> property_abstract_interface_12.cj:5:9:
  | 
5 |     prop@b: Int64
  |     ~~~~^ expected a property name here
  |     |
  |     after keyword 'prop' in property definition
  | 

error: expected declaration, found ':'
 ==> property_abstract_interface_12.cj:5:11:
  | 
5 |     prop@b: Int64
  |           ^ expected declaration here
  | 

2 errors generated, 2 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
