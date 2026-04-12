// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

var a = 0
let b = c()

class c {}

struct d {
    public init(e: Float32, f: c) {}
    public init() {
        match {
            case _ => d((a++), match {
                case _ => b
            })
        }
    }
}

/* SCAN
error: mismatched types
  ==> record_argument_unit.cj:18:26:
   | 
18 |             case _ => d((a++), match {
   |                          ^^^ expected 'Float32', found 'Unit'
   | 
   # note: the type of an assignment expression is always 'Unit'

1 error generated, 1 error printed.
*/
