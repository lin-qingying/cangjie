// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// Test for generic instantiation correctness
type BytesSlice = Array<Byte>

main() {
    let a: Array<(String, BytesSlice)> = []
    let b: Array<(String, Array<(String, (BytesSlice) -> Unit)>)> = []
}
