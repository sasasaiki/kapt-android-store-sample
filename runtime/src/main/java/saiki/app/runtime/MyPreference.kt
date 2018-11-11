package saiki.app.runtime

class MyPreference {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T,C> getInstance(target: Class<T>, context: Class<C>): IMyPreference<T, C> {
            if (context.name != "android.content.Context"){
                throw Exception("第二引数にはConst::class.javaを渡してください")
            }
            val gen = Class.forName("saiki.app.mypreference." + target.simpleName + "_Generated")
            return gen.newInstance() as IMyPreference<T, C>
        }
    }
}

interface IMyPreference<T, C> {
    fun get(context: C) : T
    fun store(target: T, context: C)
}

