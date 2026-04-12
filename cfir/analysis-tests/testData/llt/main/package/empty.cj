// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: mkdir empty
// EXEC: %touch empty/pkg.cj
// EXEC: %compiler %pkg_opt %n 2>&1 | compare %f
// ASSERT: scan-not entry function 'main' is missing
