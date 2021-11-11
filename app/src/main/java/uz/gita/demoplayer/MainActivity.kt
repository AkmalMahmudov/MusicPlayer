package uz.gita.demoplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.demoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::bind)
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadViews()
//        binding.drawerLayout.setBackgroundResource(R.color.black)
    }

    @SuppressLint("RtlHardcoded")
    private fun loadViews() {
        title = "My Player"
        toggle =
            ActionBarDrawerToggle(this, binding.drawerLayout, R.string.app_name, R.string.app_name)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.navView.setNavigationItemSelectedListener {
            binding.drawerLayout.closeDrawer(Gravity.LEFT)
            when (it.itemId) {
                R.id.item1 -> {
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }
                R.id.item2 -> {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/developer?id=GITA+Dasturchilar+Akademiyasi")
                        )
                    startActivity(intent)
                }
                R.id.item3 -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("mailto:m.akmaljon2001@gmail.com"))
                    startActivity(intent)
                }
                R.id.item4 -> {
                    this.finish()
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
