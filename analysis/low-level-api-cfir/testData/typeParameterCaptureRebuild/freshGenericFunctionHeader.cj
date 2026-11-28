// M 未声明：只验证 raw header/capture 身份，不期待语义诊断。
package lowlevel.typeparametercapture
func identity<T>(value: T): T { return @M(value) }
