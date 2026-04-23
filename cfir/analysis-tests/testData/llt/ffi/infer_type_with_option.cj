// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC-PIPE-0: %compiler %noColor %cmp_opt_chir2hlir %f -o %n.%suffix
// (CJNATIVE)EXEC-0: %run %n.%suffix
// (not CJNATIVE) ERRCHECK: %compiler %noColor %f

func malloc<T>(initial: Option<T>): Int64 {
    initial.isSome()
    0
}

main() {
    var a: Option<CString> = <!TYPE_MISMATCH!>CString(CPointer())<!>
    var b: Option<Array<Int64>> = Array<Int64>()
    malloc<CPointer<Byte>>(CPointer())
    malloc<CString>(<!TYPE_MISMATCH!>CString(CPointer())<!>)
    0
}
