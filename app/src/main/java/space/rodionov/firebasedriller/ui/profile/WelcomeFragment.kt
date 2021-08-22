package space.rodionov.firebasedriller.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.FragmentWelcomeBinding

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private val args: WelcomeFragmentArgs by navArgs()
    private val viewModel: WelcomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

        val username = args.username ?: "noname"

        binding.apply {
            tvWelcome.text = "Welcome${", "+username}!\nYou successfully ${args.madeAction}"

            btnOk.setOnClickListener {
                viewModel.ok()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.welcomeEvent.collect { event ->
                when (event) {
                    is WelcomeViewModel.WelcomeEvent.NavWelcomeToNotes -> {
                        val action = WelcomeFragmentDirections.actionWelcomeFragmentToPrivateNotesFragment()
                        findNavController().navigate(action)
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