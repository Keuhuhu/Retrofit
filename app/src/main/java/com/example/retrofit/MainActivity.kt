package com.example.retrofit // samakan dengan package proyekmu

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var listView: ListView
    private lateinit var btnCobaLagi: Button
    private lateinit var btnKirim: Button
    private lateinit var btnHapus: Button
    private lateinit var adapter: ArrayAdapter<String>

    // Disimpan agar klik item (Soal 5) tahu id pengguna yang dipilih
    private var daftarUser: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        listView = findViewById(R.id.listUser)
        btnCobaLagi = findViewById(R.id.btnCobaLagi)
        btnKirim = findViewById(R.id.btnKirim)
        btnHapus = findViewById(R.id.btnHapus)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        btnCobaLagi.setOnClickListener { muatUsers() }
        btnKirim.setOnClickListener { kirimPostingan() }
        btnHapus.setOnClickListener { hapusPostingan() }
        listView.setOnItemClickListener { _, _, position, _ ->
            muatPostsUser(daftarUser[position])
        }

        muatUsers()
    }

    private fun muatUsers() {
        tvStatus.text = "Memuat data..."
        btnCobaLagi.visibility = View.GONE

        // lifecycleScope: coroutine otomatis dibatalkan saat Activity ditutup
        lifecycleScope.launch {
            try {
                val daftar = RetrofitClient.api.getUsers()
                daftarUser = daftar

                adapter.clear()
                adapter.addAll(daftar.map { "${it.name} (@${it.username})" })
                tvStatus.text = "${daftar.size} pengguna dimuat"

            } catch (e: HttpException) {
                // Server menjawab, tetapi dengan kode error (4xx / 5xx)
                tvStatus.text = "Server error: HTTP ${e.code()}"
                btnCobaLagi.visibility = View.VISIBLE
            } catch (e: IOException) {
                // Tidak ada jawaban: internet mati, timeout, DNS gagal
                tvStatus.text = "Tidak ada koneksi internet"
                btnCobaLagi.visibility = View.VISIBLE
            }
        }
    }

    // Soal 5: klik pengguna -> ambil /users/{id}/posts
    private fun muatPostsUser(user: User) {
        tvStatus.text = "Memuat postingan ${user.name}..."

        lifecycleScope.launch {
            try {
                val posts = RetrofitClient.api.getPostsByUser(user.id)
                tvStatus.text = "${posts.size} postingan milik ${user.name}"

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Postingan ${user.name}")
                    .setItems(posts.map { it.title }.toTypedArray(), null)
                    .setPositiveButton("Tutup", null)
                    .show()

            } catch (e: HttpException) {
                tvStatus.text = "Gagal memuat postingan: HTTP ${e.code()}"
            } catch (e: IOException) {
                tvStatus.text = "Tidak ada koneksi internet"
            }
        }
    }

    // Fitur POST dari modul
    private fun kirimPostingan() {
        lifecycleScope.launch {
            try {
                val baru = PostBaru(
                    title = "Belajar Retrofit",
                    body = "Contoh POST dari aplikasi",
                    userId = 1
                )
                val hasil = RetrofitClient.api.buatPost(baru)
                tvStatus.text = "Tersimpan dengan id ${hasil.id}"
            } catch (e: HttpException) {
                tvStatus.text = "Gagal menyimpan: HTTP ${e.code()}"
            } catch (e: IOException) {
                tvStatus.text = "Tidak ada koneksi internet"
            }
        }
    }

    // Soal 4: hapus postingan (id 1 sebagai contoh)
    private fun hapusPostingan() {
        lifecycleScope.launch {
            try {
                // Return type Response<Unit>: kode 4xx/5xx TIDAK melempar HttpException,
                // jadi kode HTTP dicek manual lewat respons.code()
                val respons = RetrofitClient.api.hapusPost(1)
                tvStatus.text = if (respons.code() == 200) {
                    "Postingan berhasil dihapus"
                } else {
                    "Gagal menghapus: HTTP ${respons.code()}"
                }
            } catch (e: IOException) {
                tvStatus.text = "Tidak ada koneksi internet"
            }
        }
    }
}