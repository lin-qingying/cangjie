import { requireCJLib } from "libark_interop_loader.so";
import { register_hsp_api } from 'cjhyapiregister';

register_hsp_api()

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
          })
      }
      .width('100%')
    }
    .height('100%')
  }
}
