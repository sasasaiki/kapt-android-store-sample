package saiki.app.my_preference_compiler


import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import saiki.app.mypreference.MyPreference
import java.io.File
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

@AutoService(Processor::class)//auto-service使うのに必要なので忘れずに
class MyProcessor : BasicAnnotationProcessor() {//AbstractProcessorもしくはBasicAnnotationProcessorを継承する
companion object {
    private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"//こういうもんらしい
}

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!//コンパイラのサポートバージョンを指定

    override fun initSteps(): MutableIterable<ProcessingStep> {
        val outputDirectory = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.replace("kaptKotlin", "kapt")//ここでkaptKotlinをkaptに変えないと生成後のclassが読めない
                ?.let { File(it) }
                ?: throw IllegalArgumentException("No output directory!")

        //ここでStepたちを渡すと実行される
        return mutableListOf(MyProcessingStep(outputDir = outputDirectory))
    }
}

class MyProcessingStep(private val outputDir: File) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = mutableSetOf(MyPreference::class.java)//どのアノテーションを処理するか羅列

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?): MutableSet<Element> {
        elementsByAnnotation ?: return mutableSetOf()
        try {
            for (annotatedElement in elementsByAnnotation[MyPreference::class.java]) {

                if (annotatedElement.kind !== ElementKind.CLASS) {//今回はClassしかこないが念のためチェック
                    throw Exception("@${MyPreference::class.java.simpleName} can annotate class type.")
                }

                // fieldにつけると$が付いてくることがあるらしいのであればとる
                val annotatedClassName = annotatedElement.simpleName.toString().trimDollarIfNeeded()

                //func生成
                val generatingFunc = FunSpec
                        .builder("greet")
                        .addStatement("return \"Hello $annotatedClassName !!\"")
                        .build()

                //class生成
                val generatingClass = TypeSpec
                        .classBuilder("${annotatedClassName}_Generated")
                        .addFunction(generatingFunc)
                        .build()

                //書き込み
                FileSpec.builder("app.saiki.mypreference", generatingClass.name!!)
                        .addType(generatingClass)
                        .build()
                        .writeTo(outputDir)
            }

        } catch (e: Exception) {
            throw e
        }

        // ここで何かしらをreturnすると次のステップでごにょごにょできるらしい？
        return mutableSetOf()
    }


    // 名前に含まれる$をとる
    private fun String.trimDollarIfNeeded(): String {
        val index = indexOf("$")
        return if (index == -1) this else substring(0, index)
    }
}