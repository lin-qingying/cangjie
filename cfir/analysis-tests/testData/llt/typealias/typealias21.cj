// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f  2>&1 | compare %f
// ASSERT: regex-not warning.*are not used

type Parse<T, Result> = (Array<T>) -> ?(Result*Array<T>)

main() {
    println("Hello World!")
}
