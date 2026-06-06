// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
main() {
    VArray<Int64, $3><!parse_varray_with_paren!>6<!>([1, 2, 3, 4])
    let a = VArray<Int64, $3><!parse_varray_with_paren!>
<!>    0
}
