// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main():Int64 {
    var i2: Int64 = 0
    print("indexOf('?', 5)=" += Int32(i2).toString() + "\n")
    return 0
}

/* SCAN
error: invalid left-hand expression of assignment '+='
  ==> assign_006.cj:11:30:
   | 
11 |     print("indexOf('?', 5)=" += Int32(i2).toString() + "\n")
   |           ~~~~~~~~~~~~~~~~~~ ^^ 
   |           |
   |           cannot assign to this expression
   | 

1 error generated, 1 error printed.
*/
