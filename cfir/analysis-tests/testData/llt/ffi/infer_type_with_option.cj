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
    var a: Option<CString> = CString(CPointer())
    var b: Option<Array<Int64>> = Array<Int64>()
    malloc<CPointer<Byte>>(CPointer())
    malloc<CString>(CString(CPointer()))
    0
}

/* SCAN
error: mismatched types
  ==> infer_type_with_option.cj:11:30:
   |
11 |     var a: Option<CString> = CString(CPointer())
   |                              ^^^^^^^^^^^^^^^^^^^ expected 'Enum-Option<CString>', found 'CString'
   |

error: mismatched types
  ==> infer_type_with_option.cj:14:21:
   |
14 |     malloc<CString>(CString(CPointer()))
   |                     ^^^^^^^^^^^^^^^^^^^ expected 'Enum-Option<CString>', found 'CString'
   |

2 errors generated, 2 errors printed.
 */
