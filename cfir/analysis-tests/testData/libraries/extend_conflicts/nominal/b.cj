package b
import base.Foo
public interface IB {
    prop code: Int64 { get() { 2 } }
}
extend Foo <: IB {}
