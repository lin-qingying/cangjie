// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func bar(b:Int32):Int32
main() {
    let b: Int32 = 1
    bar(b)
    return 0
}

/* SCAN
error: body of function 'bar' is missing
 ==> funcDecl1.cj:9:24:
  | 
9 | func bar(b:Int32):Int32
  |                        ^ missing function body here
  | 

1 error generated, 1 error printed.
*/
