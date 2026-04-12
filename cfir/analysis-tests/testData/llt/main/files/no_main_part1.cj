// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: no_main_part2.cj
// EXEC: %compiler %f no_main_part2.cj 2>&1 | compare %f
// ASSERT: scan 'main' is missing

func main1() {
    return 1
}
