// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

@C
foreign func get_cptr6(cb2: CFunc<(CPointer<Int8CPointer<Int8>>): CPointer<Int8><(CPointer<Int8>)->CPointer<Int8>>): CPointer<Int8>

unsafe main():Int64 {
    return 0
}

// ASSERT: scan error: unclosed delimiter: '<'
