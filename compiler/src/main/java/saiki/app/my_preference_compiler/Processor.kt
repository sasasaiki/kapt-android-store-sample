package saiki.app.mypreferencecompiler

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.*
import saiki.app.mypreference.Savable
import java.io.File
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
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

    override fun annotations() = mutableSetOf(Savable::class.java)//どのアノテーションを処理するか羅列


    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?): MutableSet<Element> {
        messager.printMessage(Diagnostic.Kind.WARNING,"My processor start !!")

        elementsByAnnotation ?: return mutableSetOf()
        try {
            for (annotatedElement in elementsByAnnotation[Savable::class.java]) {

                if (annotatedElement.kind !== ElementKind.CLASS) {//今回はClassしかこないが念のためチェック
                    throw Exception("@${Savable::class.java.simpleName} can annotate class type.")
                }


                // fieldにつけると$が付いてくることがあるらしいのであればとる
                val annotatedClassName = annotatedElement.simpleName.toString().trimDollarIfNeeded()

                val funGet = createGetFun(annotatedElement, annotatedClassName)
                val funStore = createStoreFun(annotatedElement)


                val type = annotatedElement.asType()
                val myPrefInterface = ClassName("saiki.app.runtime","IMyPreference")
                val genericClass = ParameterizedTypeName.get(myPrefInterface,type.asTypeName())
                //class生成
                val generatingClass = TypeSpec
                        .classBuilder("${annotatedClassName}_Generated")
                        .addSuperinterface(genericClass)
                        .addFunction(funGet)
                        .addFunction(funStore)
                        .build()

                //書き込み
                FileSpec.builder("saiki.app.runtime", generatingClass.name!!)
                        .addType(generatingClass)
                        .build()
                        .writeTo(outputDir)
            }

        } catch (e: Exception) {
            throw e
        }

        messager.printMessage(Diagnostic.Kind.WARNING,"My processor finish !!")

        // ここで何かしらをreturnすると次のステップでごにょごにょできるらしい？
        return mutableSetOf()
    }


    private val getSharedPreferencesStatement = "val preferences = context.getSharedPreferences(\"DATA\", Context.MODE_PRIVATE)"
    private val context = ClassName("android.content", "Context")

    private fun createGetFun(annotatedElement: Element, annotatedClassName: String): FunSpec {

        val fieldSets = getEnclosedFields(annotatedElement)
        val setValueStatement = fieldSets.joinToString(", ") { field -> "${field.name} = ${field.name}" }

        val getFromPrefStatements = fieldSets.map {
            "val ${it.name} = preferences.getString(\"${it.name.toUpperCase()}\", \"\") ?: \"\""
        }

        val creatingInstanceStatement = "return $annotatedClassName($setValueStatement)"

        //func生成
        val type = annotatedElement.asType()
        val returnClass = ClassName.bestGuess(type.toString())
        return FunSpec
                .builder("get")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement(getSharedPreferencesStatement)
                .addStatements(getFromPrefStatements)
                .addStatement(creatingInstanceStatement)
                .addParameter("context", context)
                .returns(returnClass)
                .build()
    }


    private fun createStoreFun(annotatedElement: Element): FunSpec {

        val fieldSets = getEnclosedFields(annotatedElement)


        //func生成
        val type = annotatedElement.asType()
        val parameterClass = ClassName.bestGuess(type.toString())
        val context = ClassName("android.content", "Context")
        return FunSpec
                .builder("store")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("target", parameterClass)
                .addParameter("context", context)
                .addStatement( "val preferences = context.getSharedPreferences(\"DATA\", Context.MODE_PRIVATE)")
                .addStatement("val editor = preferences.edit()")
                .addStatements(createSotreStatements(fieldSets))
                .addStatement("editor.apply()")
                .build()
    }
    /*

    override fun store(target: User, context: Context) {
        val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("NAME", target.name)
        editor.putString("AGE", target.age)
        editor.apply()
    }

    */

    private fun createSotreStatements(fieldSets: MutableList<FieldSet>): List<String> {
        return fieldSets.map {
            "editor.putString(\"${it.name.toUpperCase()}\", target.${it.name})"
        }
    }


    private fun FunSpec.Builder.addStatements(statements: List<String>): FunSpec.Builder {
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