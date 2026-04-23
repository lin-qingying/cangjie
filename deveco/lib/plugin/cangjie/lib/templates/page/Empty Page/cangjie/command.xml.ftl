<copy src="cangjie/har/CJHybridView-v1.0.4.har" dest="${rootOut}/har/CJHybridView-v1.0.4.har"/>
<copy src="../../common/har/CJHyAPIRegister-v1.0.1.har" dest="${rootOut}/har/CJHyAPIRegister-v1.0.1.har"/>
<instantiate src="cangjie/code/page.cj.ftl" dest="${targetDir}/${pageFileName}.cj"/>
<open file="${targetDir}/${pageFileName}.cj"/>
<merge src="cangjie/config/module-oh-package.json5.ftl" dest="${rootOut}/oh-package.json5"/>
