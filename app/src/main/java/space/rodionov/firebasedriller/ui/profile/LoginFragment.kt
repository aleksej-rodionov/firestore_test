package space.rodionov.firebasedriller.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.FragmentLoginBinding
import space.rodionov.firebasedriller.hidden.WEBCLIENT_ID

private const val TAG = "LoginFragment LOGS"

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        auth = FirebaseAuth.getInstance()

        binding.apply {
            etEmail.addTextChangedListener { viewModel.em = it.toString() }
            etPassword.addTextChangedListener { viewModel.pw = it.toString() }

            etEmail.setText(viewModel.em)
            etPassword.setText(viewModel.pw)

            btnLogin.setOnClickListener {
                viewModel.loginUser()
            }

            tvSignupEmail.setOnClickListener {
                viewModel.signUp()
            }

            tvLoginGoogle.setOnClickListener {
                viewModel.onGoogleSignIn(requireContext(), WEBCLIENT_ID)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.loginEvent.collect { event ->
                when (event) {
                    is LoginViewModel.LoginEvent.NavigateLoggedIn -> {
                        val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment(
                            event.em,
                            event.pw,
                            event.ma
                        )
                        findNavController().navigate(action)
                    }
                    is LoginViewModel.LoginEvent.NavigateNotRegistered -> {
                        val action = LoginFragmentDirections.actionLoginFragmentToSignUpFragment()
                        findNavController().navigate(action)
                    }
                    is LoginViewModel.LoginEvent.LoginSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is LoginViewModel.LoginEvent.LoginActivity -> {
                        startActivityForResult(event.intent, event.requestCode)
                        Log.d(TAG, "google event collected")
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: called, result = $resultCode, requestCode = $requestCode")
        Log.d(TAG, "onActivityResult: data = $data")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_SIGN_IN) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.result
                account?.let { googleAccount ->
                    viewModel.googleAuthForFirebase(googleAccount)
                }
            }
        } else {
            viewModel.onResultCanceled()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}