package saiki.app.mypreferencecompiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import saiki.app.mypreference.MyPreference
import java.io.File
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.annotation.processing.Messager
import javax.lang.model.type.TypeMirror


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

                val funGet = createGetFun(annotatedElement, annotatedClassName)
                val funStore = createStoreFun(annotatedElement, annotatedClassName)


                //class生成
                val generatingClass = TypeSpec
                        .classBuilder("${annotatedClassName}_Generated")
                        .addFunction(funGet)
                        .addFunction(funStore)
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

    private val getSharedPreferencesStatement = "val preferences = getSharedPreferences(\"DATA\", Context.MODE_PRIVATE)"
    private fun createGetFun(annotatedElement: Element, annotatedClassName: String): FunSpec {


        val fieldSets = getEnclosedFields(annotatedElement)
        val setValueStatement = fieldSets.joinToString(", ") { field -> "${field.name} = ${field.name}" }

        val getFromPrefStatements = fieldSets.map {
            "val ${it.name} = preferences.getString(\"${it.name.toUpperCase()}\", \"\")"
        }

        val creatingInstanceStatement = "return $annotatedClassName($setValueStatement)"

        //func生成
        val type = annotatedElement.asType()
        val returnClass = ClassName.bestGuess(type.toString())
        return FunSpec
                .builder("store")
                .addStatement(getSharedPreferencesStatement)
                .addStatements(getFromPrefStatements)
                .addStatement(creatingInstanceStatement)
    //                        .addParameter("context",Context)
                .returns(returnClass)
                .build()
    }

//    fun store(user: User) {
//        val preferences = getSharedPreferences("DATA", Context.MODE_PRIVATE)
//
//        val editor = preferences.edit()
//        editor.putString("NAME", user.name)
//        editor.putInt("AGE", user.age)
//        editor.apply()
//    }
    private fun createStoreFun(annotatedElement: Element, annotatedClassName: String): FunSpec {

        val fieldSets = getEnclosedFields(annotatedElement)

    val argName = annotatedClassName.toLowerCase()
    val storeForPrefStatements = fieldSets.map {
            "editor.putString(\"${it.name.toUpperCase()}\", $argName.${it.name})"
        }

        //func生成
        val type = annotatedElement.asType()
        val parameterClass = ClassName.bestGuess(type.toString())
        return FunSpec
                .builder("get")
                .addParameter(argName,parameterClass)
                .addStatement(getSharedPreferencesStatement)
                .addStatement("val editor = preferences.edit()")
                .addStatements(storeForPrefStatements)
                .build()
    }

    private fun FunSpec.Builder.addStatements(statements : List<String>) : FunSpec.Builder {
        statements.forEach { this.addStatement(it) }
        return this
    }

    private fun getEnclosedFields(element: Element): MutableList<FieldSet> {
        val fields = mutableListOf<FieldSet>()
        for (el in element.enclosedElements) {
            if (el.kind == ElementKind.FIELD) {
                val fieldSet = FieldSet(
                        el.simpleName.toString(),
                        el.asType()
                )

                fields.add(fieldSet)
            }
        }

        return fields
    }


    data class FieldSet(val name: String, val type: TypeMirror)

    // 名前に含まれる$をとる
    private fun String.trimDollarIfNeeded(): String {
        val index = indexOf("$")
        return if (index == -1) this else substring(0, index)
    }
}