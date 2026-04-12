// (CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ast %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    // UInt32_Max = 4294967295
    var a = VArray<Int64, $4294967297>(repeat: 0)
    a[4294967296] // pass
}

/* SCAN-IN
            VarDecl: var a {
              curFile: varray_index04.cj
              position: (1, 5, 5) (1, 5, 50)
              scopeName: "a0a0a"
              ty: VArray<Int64, $4294967297>
*/
/* SCAN-IN
              VArrayExpr {
                curFile: varray_index04.cj
                position: (1, 5, 13) (1, 5, 50)
                scopeName: "a0a0a"
                ty: VArray<Int64, $4294967297>
*/
/* SCAN-IN
                args [
                  FuncArg {
                    curFile: varray_index04.cj
                    position: (1, 5, 40) (1, 5, 49)
                    scopeName: "a0a0a"
                    ty: Int64
*/

/* SCAN-IN
              SubscriptExpr {
                curFile: varray_index04.cj
                position: (1, 6, 5) (1, 6, 18)
                scopeName: "a0a0a"
                ty: Int64
*/

/* SCAN-IN
                indexExprs [
                  LitConstExpr: Integer "4294967296" {
                    curFile: varray_index04.cj
                    position: (1, 6, 7) (1, 6, 17)
                    scopeName: "a0a0a"
                    ty: Int64
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
