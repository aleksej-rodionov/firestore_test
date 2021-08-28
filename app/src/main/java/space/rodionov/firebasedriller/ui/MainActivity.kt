package space.rodionov.firebasedriller.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.ActivityMainBinding
import space.rodionov.firebasedriller.databinding.NavHeaderBinding
import space.rodionov.firebasedriller.ui.privatenotes.OnCheckLoginState

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnCheckLoginState {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.findNavController()

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.privateNotesFragment, R.id.sharedNotesFragment, R.id.settingsFragment, R.id.profileFragment),
            binding.drawerLayout
        )

        setSupportActionBar(binding.toolbarMain) // only if we use toolbar but not the normal actionbar
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navView.setupWithNavController(navController)

        val headerView = binding.navView.getHeaderView(0)
        val headerBinding = NavHeaderBinding.bind(headerView)
        headerBinding.apply {
            lifecycleScope.launchWhenStarted {
                viewModel.userDataFlow.collect {
                    tvUsername.text = it.first
                    tvEmail.text = it.second
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun checkLoginState(isLoggedIn: Boolean) {
        viewModel.checkIfLoggedIn(isLoggedIn)
    }
}

const val ADD_NOTE_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_NOTE_RESULT_OK = Activity.RESULT_FIRST_USER + 1



