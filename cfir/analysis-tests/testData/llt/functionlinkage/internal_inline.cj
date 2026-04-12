// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ir-to-screen %compile_lib_opt %enableO2 --disable-reflection %f 2>&1 | compare %f
// (not CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ir-to-screen %compile_lib_opt %enableO2 %f 2>&1 | compare %f
// (CJNATIVE) ASSERT: regex-not internal.*@_CN3pkg3fooEv
// (not CJNATIVE) ASSERT: regex-not internal.*@"_CN3pkg16\$internal_inline3fooEv"

package pkg

private func foo() {
    1
}

// main will not be source exported.
main() {
    foo()
}
