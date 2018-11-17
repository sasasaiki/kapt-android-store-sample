package saiki.app.devoxxkapt

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import saiki.app.mypreference.Savable
import saiki.app.runtime.User_Generated

@Savable
data class User(
        val name: String,
        val age: String
)

fun store(target: User, context: Context) {
    val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
    val editor = preferences.edit()
    editor.putString("NAME", target.name)
    editor.putString("AGE", target.age)
    editor.apply()
}

fun get(context: Context): User {
    val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
    val name = preferences.getString("NAME", "") ?: ""
    val age = preferences.getString("AGE", "") ?: ""
    return User(name = name, age = age)
}


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pref = User_Generated()
        pref.store(User("",""),this)
        val user = pref.get(this)
    }

}




