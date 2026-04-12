// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A<T> {
    mut prop a: String {
        get() {
            b
        }
        set(v) {
            b = v
        }
    }
    var b: String = "a${a}"
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> class_initialization6.cj:18:25:
   | 
18 |     var b: String = "a${a}"
   |                         ^ 
   | 

1 error generated, 1 error printed.
*/
