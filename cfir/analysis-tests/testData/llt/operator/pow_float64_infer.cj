// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt --int-overflow wrapping %f -o %output
// EXEC-PIPE: %run %run_opt %output %run_args | compare %f
// ASSERT:scan 1024.0

func f(x: Int64): Float64 {
    10.0
}

func f(x: Int32): Float32 {
    -10.0
}

func f(x: Int16): Float16 {
    -10.0
}

func g(x: Int64): Int64 {
    10
}

func g(x: Int32): Int32 {
    -10
}

func g(x: Int16): Int16 {
    -10
}

main() {
    let a = 2.0 ** f(2)
    println(a)
    
    0
}
