// (DEBUG_OPTION) RUN-EXEC-PIPE-0: %frontendCompiler %dump-ast %cmp_opt %f 2>&1 | compare %f

let b: VArray<Int64, $3> = VArray<Int64, $3>(repeat: 0)

main() {}



/* SCAN-IN
    VarDecl: let b {
      curFile: varray_print_ast.cj
      position: (1, 3, 1) (1, 3, 56)
      scopeName: "a"
      ty: VArray<Int64, $3>
*/

/* SCAN-IN
      VArrayType {
        curFile: varray_print_ast.cj
        position: (1, 3, 8) (1, 3, 25)
        scopeName: "a"
        ty: VArray<Int64, $3>
*/

/* SCAN-IN
      // initializer
      VArrayExpr {
        curFile: varray_print_ast.cj
        position: (1, 3, 28) (1, 3, 56)
        scopeName: "a"
        ty: VArray<Int64, $3>
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
