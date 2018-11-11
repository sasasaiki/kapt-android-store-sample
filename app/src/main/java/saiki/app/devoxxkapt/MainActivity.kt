package saiki.app.devoxxkapt

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import saiki.app.mypreference.Savable
import saiki.app.runtime.MyPreference

@Savable
data class User(
        val name: String,
        val age: String
)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = getUser(context = this)

        store_name_button.setOnClickListener {
            val newUser = user.copy(name = name_text_area.text.toString())
            storeUser(newUser, this)
        }

        val pref = MyPreference.getInstance(User::class.java)
        val foo = pref.get(context = this)
        pref.store(foo.copy(name = "new"),this)

        /*
        //保存されているユーザーを取ってくる
        val user = MyPreferences.get(User::class.java)

        //ボタンが押されたら新しい名前を保存する
        save_name_button.setOnClickListener {
            val newName = name_text_field
            val newUser = user.copy(name = newName.text)
            MyPreferences.store(newUser)
        }
        */

    }

    private fun getUser(context: Context): User {
        val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
        val name = preferences.getString("NAME", "") ?: ""
        val age = preferences.getString("AGE", "") ?: ""
        return User(name = name, age = age)
    }

    private fun storeUser(user: User, context: Context) {
        val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("NAME", user.name)
        editor.putString("AGE", user.age)
        editor.apply()
    }

}




