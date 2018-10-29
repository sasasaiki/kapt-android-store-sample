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
import com.squareup.kotlinpoet.ClassName
import saiki.app.mypreference.SavingField
import javax.annotation.processing.Messager
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic


@AutoService(Processor::class)//auto-service使うのに必要なので忘れずに
class MyProcessor : BasicAnnotationProcessor() {
    //AbstractProcessorもしくはBasicAnnotationProcessorを継承する
    companion object {
        private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"//こういうもんらしい
    }

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!//コンパイラのサポートバージョンを指定

    override fun initSteps(): MutableIterable<ProcessingStep> {
        val outputDirectory = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.replace("kaptKotlin", "kapt")//ここでkaptKotlinをkaptに変えないと生成後のclassが読めない
                ?.let { File(it) }
                ?: throw IllegalArgumentException("No output directory!")

        val messager = super.processingEnv.messager


        //ここでStepたちを渡すと実行される
        return mutableListOf(MyProcessingStep(outputDir = outputDirectory, messager = messager))
    }
}

class MyProcessingStep(private val outputDir: File, private val messager: Messager) : BasicAnnotationProcessor.ProcessingStep {

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

                val type = annotatedElement.asType()
                val returnClass = ClassName.bestGuess(type.toString())

                messager.printMessage(Diagnostic.Kind.WARNING, "return class" + returnClass.toString())
                messager.printMessage(Diagnostic.Kind.WARNING, "type name ? " + type.toString())


                val getMethod = """
                    return $annotatedClassName(name = "aa", age = 10)
                """.trimIndent()

                enclosed(element = annotatedElement)

                //func生成
                val generatingFunc = FunSpec
                        .builder("get")
                        .addStatement(getMethod)
                        .returns(returnClass)
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

    private fun getEnclosedFields(element: Element): List<FieldSet> {
        val fields = mutableListOf<FieldSet>()
        for (el in element.enclosedElements) {
            if (el.kind == ElementKind.FIELD) {
                fields.add(
                        FieldSet(
                                el.simpleName.toString(),
                                el.asType()
                        )
                )
            }
            val fieldName = el.simpleName.toString()


            messager.printMessage(Diagnostic.Kind.WARNING, "type ${el.asType()}")
            messager.printMessage(Diagnostic.Kind.WARNING, "kind ${el.kind}")
            messager.printMessage(Diagnostic.Kind.WARNING, "名前 $fieldName")

        }
    }

    data class FieldSet(val name: String, val type: TypeMirror)

    // 名前に含まれる$をとる
    private fun String.trimDollarIfNeeded(): String {
        val index = indexOf("$")
        return if (index == -1) this else substring(0, index)
    }
}