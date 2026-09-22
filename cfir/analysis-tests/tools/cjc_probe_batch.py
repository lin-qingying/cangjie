#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""临时批量探针：把一组最小构造喂给官方 cjc（1.0.5 / 1.0.0）。"""

from __future__ import annotations

import sys

sys.path.insert(0, r"D:\code\intellij\cangjie\cfir\analysis-tests\tools")
from cjc_one_probe import probe  # noqa: E402

PROBES = {
    # 1. 元组解构目标类型 (Int64,Bool) vs 字面量元组 (true,false)
    "tuple_literal": """main() {
    let (c1, c2): (Int64, Bool) = (true, false)
    0
}
""",
    # 2. 四字符字面量 -> Int64：CANNOT_CONVERT_LITERAL 的锚点宽度
    "literal_bool_to_int": """main() {
    let z: Int64 = true
    0
}
""",
    # 3. 元组元素不匹配（非字面量）
    "tuple_element_mismatch": """main() {
    let (c1, c2): (Int64, Bool) = (1, false)
    0
}
""",
    # 4. 元组元数不匹配
    "tuple_arity_mismatch": """main() {
    let (c1, c2): (Int64, Bool) = (1, 2, 3)
    0
}
""",
    # 5. coalescing：RHS 字面量与 LHS 解包类型不符 + 外层运算符
    "coalescing_with_op": """main() {
    (Some(true) ?? 1) * false
    0
}
""",
    # 6. coalescing 单独出现（无外层运算符）
    "coalescing_alone": """main() {
    Some(true) ?? 1
    0
}
""",
    # 7. 上界非法：族内常规形状（上界是 class+class）
    "upperbound_class_class": """func foo1<T>() where T <: Int32 & Bool {}
""",
    # 8. 上界非法：上界含类型参数（type_arg_infer5 的形状）
    "upperbound_typeparam": """class A {}
func f<T>() where T <: A {
    func g<R>(): R where R <: T & A {
        let v: Int64 = g()
        return g<R>()
    }
}
main() {
}
""",
    # 9. 泛型推断失败的锚点（callee vs 整个调用）
    "unable_infer_callee": """class C {}
func f<T>(x: T) where T <: Comparable<T> {
}
main() {
    f(C())
}
""",
    # 10. 元组解构：元素为非字面量且类型不匹配
    "tuple_element_nonliteral": """func g(): Bool { true }
main() {
    let (c1, c2): (Int64, Bool) = (g(), false)
    0
}
""",
    # 11. 元组解构：元素为字面量且不匹配（无解构？直接用元组类型标注）
    "tuple_literal_int_to_bool": """main() {
    let (c1, c2): (Bool, Bool) = (1, false)
    0
}
""",
    # 12. coalescing：RHS 为非字面量的类型不匹配
    "coalescing_nonliteral_rhs": """func g(): Int64 { 1 }
main() {
    let b: Bool = Some(true) ?? g()
    0
}
""",
    # 13. coalescing：RHS 字面量 + 后续运算符（另一形态）
    "coalescing_then_member": """main() {
    let x = (Some(true) ?? 1) * false
    0
}
""",
    # 14. 二元运算符：一个操作数本身是错误（非字面量错误）
    "binary_with_unknown_operand": """main() {
    let x = undefinedName * false
    0
}
""",
    # 15. 赋值右侧元组字面量与声明类型不匹配（非解构）
    "tuple_literal_whole": """main() {
    var t: (Int64, Bool) = (true, false)
    0
}
""",
}


def main() -> None:
    sdk = sys.argv[1] if len(sys.argv) > 1 else "1.0.5"
    names = sys.argv[2:] or list(PROBES)
    for name in names:
        code, out = probe(PROBES[name], sdk)
        print("=" * 76)
        print(f"### {name}  (sdk={sdk}, exit={code})")
        print(out.strip())


if __name__ == "__main__":
    main()
