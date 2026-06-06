// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

<!sema_inheritance_cycle!>i<!>nterface A <: B {}

interface C <: A {}

interface B <: C {}

extend <!sema_illegal_extended_type!>A<!> <: C {}

main() {
    0
}
