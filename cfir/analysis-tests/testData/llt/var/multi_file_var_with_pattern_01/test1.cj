// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: test2.cj
// EXEC: %compiler %cmp_opt %f test2.cj -o %output
// EXEC: %run %run_opt %output

// Test for using decl defined by varWithPatternDecl which is in the file the has bigger fileID than current fileID.
func test() {
    let a = A
}

main() {
    test()
}
