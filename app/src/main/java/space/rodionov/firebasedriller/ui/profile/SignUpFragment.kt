package space.rodionov.firebasedriller.ui.profile

import android.os.Bundle
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
import space.rodionov.firebasedriller.databinding.FragmentRegisterBinding

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_register) {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.apply {
            etUsername.addTextChangedListener { viewModel.un = it.toString() }
            etEmail.addTextChangedListener { viewModel.em = it.toString() }
            etPassword.addTextChangedListener { viewModel.pw = it.toString() }

            etUsername.setText(viewModel.un)
            etEmail.setText(viewModel.em)
            etPassword.setText(viewModel.pw)

            btnSignUp.setOnClickListener {
                viewModel.registerUserEmailAndPassword()
//                etUsername.clearFocus()
//                etEmail.clearFocus()
                etPassword.clearFocus()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.signUpEvent.collect { event ->
                when (event) {
                    is SignUpViewModel.SignUpEvent.NavigateSignedUp -> {
                        val action = SignUpFragmentDirections.actionSignUpFragmentToWelcomeFragment(event.em, event.pw, event.ma)
                        findNavController().navigate(action)
                    }
                    is SignUpViewModel.SignUpEvent.SignUpSnackbar -> {
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