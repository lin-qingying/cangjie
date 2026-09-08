package b
public interface IB {
    prop code: Int64 { get() { 2 } }
}
extend Int64 <: IB {}
public class Unrelated {}
