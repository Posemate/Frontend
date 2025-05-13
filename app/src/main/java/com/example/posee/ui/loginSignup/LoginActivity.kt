package com.example.posee.ui.loginSignup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.posee.R
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.example.posee.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        databaseRef = FirebaseDatabase.getInstance().reference.child("Users")

        val etLoginId = findViewById<EditText>(R.id.et_login_id)
        val etLoginPassword = findViewById<EditText>(R.id.et_login_password)
        val btnLogin = findViewById<Button>(R.id.bt_login)
        val tvSignup = findViewById<TextView>(R.id.tv_login_text2)

        btnLogin.setOnClickListener {
            val userId = etLoginId.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()

            Toast.makeText(this, "클릭", Toast.LENGTH_SHORT).show()

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase에서 아이디와 비밀번호 확인
            databaseRef.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val storedPassword = snapshot.child("password").value.toString()
                    if (storedPassword == password) {
                        // 로그인 성공 후 SharedPreferences에 사용자 ID 저장
                        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("logged_in_userId", userId)
                            apply()
                        }

                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        // 로그인 후 이동할 화면
                        finish()
                    } else {
                        Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "존재하지 않는 아이디입니다.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}
