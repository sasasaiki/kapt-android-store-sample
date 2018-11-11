package saiki.app.runtime

import android.content.Context

class MyPreference {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> getInstance(target: Class<T>): IMyPreference<T> {
            val gen = Class.forName("saiki.app.runtime." + target.simpleName + "_Generated")
            return gen.newInstance() as IMyPreference<T>
        }
    }
}

interface IMyPreference<T> {
    fun get(context: Context) : T
    fun store(target: T, context: Context)
}

