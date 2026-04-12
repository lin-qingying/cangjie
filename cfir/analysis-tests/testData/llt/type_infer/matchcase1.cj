// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (DEBUG_OPTION)EXEC:%frontendCompiler %typecheck %cmp_opt %f

enum TimeUnit {
    Time(Int32, Int32)
}

main() {
    let x: Int32 = 1
    match (Time(1, 12)) {
        case x => 0
    }
    return 0
}
