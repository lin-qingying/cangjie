// DEPENDENCE: ./a.cj ./b.cj

package c
<!UNUSED_IMPORT!>import a.X<!>
<!UNUSED_IMPORT!>import a.I<!>
import b.A

main() {
   var a = Array<A>()
   return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
