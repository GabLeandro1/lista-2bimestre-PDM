package com.example.lista

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var rgOptions: RadioGroup
    private lateinit var tvResult: TextView
    private lateinit var btnSubmit: Button

    private lateinit var database: FirebaseDatabase
    private lateinit var enqueteRef: DatabaseReference

    private var selectedOptionKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.menu)

        FirebaseDatabase.getInstance().reference.keepSynced(true)

        Log.d("Firebase", "Firebase inicializado com sucesso!")
        tvQuestion = findViewById(R.id.tvQuestion)
        rgOptions = findViewById(R.id.rgOptions)
        tvResult = findViewById(R.id.tvResult)
        btnSubmit = findViewById(R.id.btnSubmit)

        database = FirebaseDatabase.getInstance("https://lista-f9f22-default-rtdb.firebaseio.com")
        enqueteRef = database.getReference("enquetes/enquete1")

        carregarEnquete()

        btnSubmit.setOnClickListener {
            if (selectedOptionKey != null) {
                votar(selectedOptionKey!!)
            } else {
                Toast.makeText(this, "Selecione uma opção", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarEnquete() {
        enqueteRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pergunta = snapshot.child("pergunta").getValue(String::class.java)
                tvQuestion.text = pergunta ?: "Pergunta não encontrada"

                rgOptions.removeAllViews()
                tvResult.text = ""

                val opcoes = snapshot.child("opcoes")
                for (opcaoSnapshot in opcoes.children) {
                    val titulo = opcaoSnapshot.child("Titulo").getValue(String::class.java)
                    val votos = opcaoSnapshot.child("QtdVotos").getValue(Int::class.java) ?: 0
                    val key = opcaoSnapshot.key

                    if (titulo != null && key != null) {
                        val radioButton = RadioButton(this@MainActivity)
                        radioButton.text = "$titulo ($votos votos)"
                        radioButton.tag = key
                        rgOptions.addView(radioButton)
                    }
                }

                rgOptions.setOnCheckedChangeListener { group, checkedId ->
                    val radioButton = findViewById<RadioButton>(checkedId)
                    selectedOptionKey = radioButton.tag as? String
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Erro: ${error.message}")
            }
        })
    }

    fun votar(opcaoKey: String) {
        val votosRef = enqueteRef.child("opcoes").child(opcaoKey).child("QtdVotos")
        votosRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var votos = currentData.getValue(Int::class.java) ?: 0
                currentData.value = votos + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    Toast.makeText(this@MainActivity, "Voto computado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Erro ao votar", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val db = FirebaseDatabase.getInstance()
        db.getReference("enquetes/enquete1/pergunta")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirebaseLog", "Pergunta: ${snapshot.value}")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseLog", "Erro ao buscar pergunta: ${e.message}")
            }
    }
}
