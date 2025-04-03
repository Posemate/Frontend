package com.example.posee.ui.loginSignup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.posee.R
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        databaseRef = FirebaseDatabase.getInstance().reference.child("Users")

        val etUsername = findViewById<EditText>(R.id.et_signup_username)
        val etId = findViewById<EditText>(R.id.et_signup_id)
        val etPassword = findViewById<EditText>(R.id.et_signup_password)
        val etPassword2 = findViewById<EditText>(R.id.et_signup_password2)
        val btnSignup = findViewById<Button>(R.id.bt_signup)
        val tvLogin = findViewById<TextView>(R.id.tv_signup_text2)

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val userId = etId.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val password2 = etPassword2.text.toString().trim()

            if (username.isEmpty() || userId.isEmpty() || password.isEmpty() || password2.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != password2) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 데이터베이스에 사용자 정보 저장
            val userMap = mapOf(
                "username" to username,
                "userId" to userId,
                "password" to password // 보안 강화를 위해 실제 앱에서는 암호화 필요
            )

            databaseRef.child(userId).setValue(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "회원가입 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
