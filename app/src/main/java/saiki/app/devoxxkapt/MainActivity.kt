package saiki.app.devoxxkapt

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

        val pref = MyPreference.getInstance(User::class.java)


        val user = pref.get(context = this)

        store_name_button.setOnClickListener {
            pref.store(user.copy(name = "new"),this)
        }
    }

}




