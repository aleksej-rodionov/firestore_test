package space.rodionov.firebasedriller.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.FragmentProfileBinding
import space.rodionov.firebasedriller.ui.privatenotes.OnCheckLoginState

private const val TAG = "HomeFragment LOGS"



@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.apply {

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.loggedInState.collect { user ->
                    Log.d(TAG, "user = $user")
                    if (user != null) {
                        tvLoginState.text = "You are logged in"
                        cvUsername.visibility = View.VISIBLE // try to delete it and check if still works
                        etUsername.setText(viewModel.un)
                        btnLogin.text = "Log out"

                        etUsername.addTextChangedListener {
                            viewModel.un = it.toString()
                        }
                        btnUpdate.setOnClickListener {
                            viewModel.updateUsername()
                            etUsername.clearFocus()
                        }
                        btnLogin.setOnClickListener {
                            viewModel.onLogout()
                            val listener = activity as OnCheckLoginState
                            listener.checkLoginState(false)
                        }
                    } else {
                        tvLoginState.text = "You are not logged in"
                        cvUsername.visibility = View.GONE
                        btnLogin.text = "Login"

                        btnLogin.setOnClickListener {
                            viewModel.onLogin()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.profileEvent.collect { event ->
                when (event) {
                    is ProfileViewModel.ProfileEvent.NavigateToLogin -> {
                        val action = ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
                        findNavController().navigate(action)
                    }
                    is ProfileViewModel.ProfileEvent.ProfileSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





