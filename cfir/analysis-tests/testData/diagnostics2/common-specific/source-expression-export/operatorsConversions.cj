package source_operators_conversions

public open class ConversionBase {}
public class ConversionLeaf <: ConversionBase {}

public interface OperatorsConversions {
    func calculate(input: Int64, values: Array<Int64>): Int64 {
        var accumulator = input
        accumulator = -accumulator
        accumulator += 2
        let pair = (accumulator, !false)
        let items = [pair[0], values[0]]
        values[0] = items[1]
        let converted = Int32(pair[0])
        return Int64(converted) + items[0]
    }

    // is 返回 Bool；as 返回 Option<ConversionLeaf>，不能写成强制转换。
    func inspect(value: ConversionBase): (Bool, Option<ConversionLeaf>) {
        (value is ConversionLeaf, value as ConversionLeaf)
    }

    func select(pair: (Int64, Int64)): Int64 {
        let (left, right) = pair
        match (left) {
            case 0 => right
            case _ => left
        }
    }

    func predicate(input: Int64): Int64 {
        match {
            case input > 0 => 1
            case _ => 0
        }
    }
}
