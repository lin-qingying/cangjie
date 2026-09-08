package a
import base.Foo
public interface IA {
    prop code: Int64 { get() { 1 } }
}
extend Foo <: IA {}
