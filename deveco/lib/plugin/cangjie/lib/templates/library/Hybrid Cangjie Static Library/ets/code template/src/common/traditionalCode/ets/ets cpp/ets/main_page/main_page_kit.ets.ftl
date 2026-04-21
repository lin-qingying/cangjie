import { hilog } from '@kit.PerformanceAnalysisKit';
import testNapi from 'lib${moduleName?lower_case}.so';
import { requireCJLib } from "libark_interop_loader.so";

interface CJLib {
  testCJ(src: string): string
}

@Component
export struct ${"${pagePackageName}"?cap_first} {
  @State message: string = 'Hello World';

  build() {
    Row() {
      Column() {
        Text(this.message)
          .fontSize(50)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
              const lib = requireCJLib("libohos_app_cangjie_${moduleName}.so") as CJLib
              this.message = lib.testCJ("Cangjie")
              hilog.info(0x0000, 'testTag', 'Test NAPI 2 + 3 = %{public}d', testNapi.add(2, 3));
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}