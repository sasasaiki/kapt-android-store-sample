package saiki.app.devoxxkapt

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import saiki.app.mypreference.MyPreference
import saiki.app.mypreference.SavingField


@MyPreference
data class User(
        @SavingField
        val name: String,
        @SavingField
        val age: String
)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    fun get(context: Context): User {
        val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
        val name = preferences.getString("NAME", "") ?: ""
        val age = preferences.getString("AGE", "") ?: ""
        return User(name = name, age = age)
    }

    fun store(user: User, context: Context) {
        val preferences = context.getSharedPreferences("DATA", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("NAME", user.name)
        editor.putString("AGE", user.age)
        editor.apply()
    }

}




