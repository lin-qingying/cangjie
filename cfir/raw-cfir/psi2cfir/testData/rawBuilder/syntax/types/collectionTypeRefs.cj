// RUN_PIPELINE_TILL: FRONTEND
// 集合类型引用：`HashMap` / `HashSet` / `ArrayList` / `LinkedList` / `Iterator` / `Iterable` / `Option`（需 `import std.collection.*`；不 import 时未声明，见 recovery）。
// 官方依据：Cangjie 文档《集合》；cjc 1.0.5 / 1.1.3 零诊断。
import std.collection.*

func collectionTypeRefs(
    m: HashMap<Int64, String>,
    s: HashSet<Int64>,
    l: ArrayList<Int64>,
    k: LinkedList<Int64>,
    it: Iterator<Int64>,
    ie: Iterable<Int64>,
    o: Option<Int64>,
): Int64 {
    let _ = (m, s, l, k, it, ie, o)
    return m.size + s.size + l.size + k.size
}
