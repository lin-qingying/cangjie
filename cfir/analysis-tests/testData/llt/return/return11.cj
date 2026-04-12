// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan-not Internal Compiler Error

var a = 'a'-o %n.%{ =>
var a = 'a'-o %n.%suffix

func run(nt64 {
    var a = 'a'
    var b = 'b'
    let c = a <= b ? a : nt6b
    if ( c == 'a'){
        return 0
    }
    .eturn 1
}

main(): Int64 {
    return run()
}