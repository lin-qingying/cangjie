// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
abstract class Boo{
    public mut prop m: blic mut prop m: Int32 {
        get() {
            return 2
        }
        set(v){}
    }
    public prop n: Int32 {
        get() {
            return 1
        }
    }
}

main() {}

/* SCAN
error: expected ';' or '<NL>', found keyword 'mut'
 ==> redef_property.cj:5:29:
  | 
5 |     public mut prop m: blic mut prop m: Int32 {
  |                             ^^^ expected ';' or '<NL>' here
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
