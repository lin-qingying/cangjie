// raw local owner 保留显式类型注解；三个 leaf 均直接首查 IMPLICIT_TYPES。
// 包含函数的 BODY_RESOLVE 必须由 leaf 的生产解析路径触发。
package test

func localOwnerHost(): Unit {
    let (number, (flag, text)): (Int64, (Bool, String)) = (1, (true, "ready"))
    ()
}
