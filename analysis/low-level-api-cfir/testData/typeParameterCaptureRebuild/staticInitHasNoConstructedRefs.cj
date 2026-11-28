// 官方 cjc 1.1.3 在 T 处报告 sema_static_variable_use_generic_parameter。
// 本例只检查 raw static init 的参数 refs；不执行语义诊断检查。
package lowlevel.typeparametercapture
class InvalidStatic<T> {
    static init() { func local(value: T): Unit {} }
}
