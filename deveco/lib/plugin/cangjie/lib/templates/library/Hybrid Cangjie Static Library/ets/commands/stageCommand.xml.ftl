<copy src="ets/code template/src/stageMode/hvigorfile.ts" dest="${rootOut}/hvigorfile.ts"/>
<#--  传统开发模式  -->
<#if !superVisualEnable>
    <#-- common -->
    <instantiate src="ets/code template/src/stageMode/module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>
    <instantiate src="ets/code template/src/common/traditionalCode/build-profile.json5.ftl" dest="${rootOut}/build-profile.json5"/>
    <#if hasOhPackageJson5 ! false>
        <instantiate src="ets/code template/src/common/traditionalCode/oh-package.json5.ftl" dest="${rootOut}/oh-package.json5"/>
    <#else>
        <instantiate src="ets/code template/src/common/traditionalCode/package.json.ftl" dest="${rootOut}/package.json"/>
    </#if>
    <#if uiSyntax?lower_case == "ets">
        <#if createUnitTest>
            <copy src="ets/code template/test" dest="${rootOut}/src/test" />
        </#if>
    <#-- ohosTest -->
        <#if compatibleBaseApi?? && compatibleBaseApi gte 11>
            <copy src="ets/code template/ohosTest/kit" dest="${rootOut}/src/ohosTest/ets" />
        <#else>
            <copy src="ets/code template/ohosTest/ets" dest="${rootOut}/src/ohosTest/ets" />
        </#if>
        <instantiate src="ets/code template/ohosTest/module.json5.ftl" dest="${rootOut}/src/ohosTest/module.json5"/>
        <#if addObfuscationConfig>
            <copy src="ets/code template/src/common/traditionalCode/obfuscation-rules.txt" dest="${rootOut}/obfuscation-rules.txt" />
            <copy src="ets/code template/src/common/traditionalCode/consumer-rules.txt" dest="${rootOut}/consumer-rules.txt" />
        </#if>
    <#--  res  -->
        <copy src="ets/code template/src/common/traditionalCode/ets/res/element/string.json" dest="${rootOut}/src/main/resources/base/element/string.json"/>
        <copy src="ets/code template/src/common/traditionalCode/ets/res/element/float.json" dest="${rootOut}/src/main/resources/base/element/float.json"/>
    <#--  src  -->
        <instantiate src="ets/code template/src/common/traditionalCode/ets/index.ets.ftl" dest="${rootOut}/Index.ets"/>
        <#if enableNative>
            <copy src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/Index.d.ts"
                  dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/Index.d.ts"/>
            <instantiate src="ets/code template/src/common/traditionalCode/cpp/napi_init.cpp.ftl"
                  dest="${rootOut}/src/main/cpp/napi_init.cpp"/>
            <instantiate src = "ets/code template/src/common/traditionalCode/cpp/CMakeLists.txt.ftl"
                         dest="${rootOut}/src/main/cpp/CMakeLists.txt"/>
            <#if compatibleBaseApi?? && compatibleBaseApi gte 11>
                <instantiate src="ets/code template/src/common/traditionalCode/ets/ets cpp/ets/main_page/main_page_kit.ets.ftl"
                  dest="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
            <#else>
                <instantiate src="ets/code template/src/common/traditionalCode/ets/ets cpp/ets/main_page/main_page.ets.ftl"
                  dest="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
            </#if>
            
            <#if hasOhPackageJson5 ! false>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/oh-package.json5.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/oh-package.json5"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-oh-package.json.ftl" dest="${rootOut}/oh-package.json5"/>
            <#else>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/package.json.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/package.json"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-package.json.ftl" dest="${rootOut}/package.json"/>
            </#if>
        <#else>
            <instantiate src="ets/code template/src/common/traditionalCode/ets/ets/main_page/main_page.ets.ftl"
                         dest="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
        </#if>
        <open file="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">
    <#--  res  -->
        <copy src="ets/code template/src/common/traditionalCode/js/res/element/string.json" dest="${rootOut}/src/main/resources/base/element/string.json"/>
    <#--  src  -->
        <copy src="ets/code template/src/common/traditionalCode/js/index.js" dest="${rootOut}/index.js"/>
        <copy src="ets/code template/src/common/traditionalCode/js/js/common/images" dest="${rootOut}/src/main/js/common/images"/>
        <copy src="ets/code template/src/common/traditionalCode/js/js/components" dest="${rootOut}/src/main/js/components"/>
        <copy src="ets/code template/src/common/traditionalCode/js/js/i18n" dest="${rootOut}/src/main/js/i18n"/>
        <#if enableNative>
            <copy src="ets/code template/src/common/traditionalCode/js/js cpp/cpp" dest="${rootOut}/src/main/cpp"/>
            <instantiate src="ets/code template/src/common/traditionalCode/js/js cpp/js/index.js.ftl"
                         dest="${rootOut}/src/main/js/components/index/index.js"/>
            <copy src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/index.d.ts"
                  dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/index.d.ts"/>
            <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/package.json.ftl"
                         dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/package.json"/>
            <instantiate src="ets/code template/src/common/traditionalCode/cpp/napi_init.cpp.ftl"
                  dest="${rootOut}/src/main/cpp/napi_init.cpp"/>
            <instantiate src = "ets/code template/src/common/traditionalCode/cpp/CMakeLists.txt.ftl"
                         dest="${rootOut}/src/main/cpp/CMakeLists.txt"/>
            <merge src="ets/code template/src/common/traditionalCode/cpp-package.json.ftl" dest="${rootOut}/package.json"/>
        </#if>
        <open file="${rootOut}/src/main/js/components/index/index.js"/>
    </#if>
<#else>
<#--  低代码开发模式  -->
    <#if uiSyntax?lower_case == "ets">

    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">

    </#if>
</#if>