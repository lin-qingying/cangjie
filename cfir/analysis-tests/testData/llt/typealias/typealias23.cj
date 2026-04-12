// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// Test for generic instantiation correctness
class A<T> {}

type MMap<T> = Array<A<A<T>>>

func test<K>() {
    let v = MMap<K>()
}

main() {
    let k = test<Int64>()
    return 0
}
