package space.rodionov.firebasedriller.ui.profile

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.model.MediaFile
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.FragmentProfileBinding
import space.rodionov.firebasedriller.ui.OnCheckLoginState

private const val TAG = "HomeFragment LOGS"


@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val cameraPermissionDialogLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.pickImage(requireContext())
            } else {
                // when permission denied - display snackbar
                viewModel.profileSnackbar("Permission denied")
            }
        }

    private val imagePickerActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_CANCELED && result.data != null) {
                val data = result.data
                val mediaFiles = data?.getParcelableArrayListExtra<MediaFile>(
                    FilePickerActivity.MEDIA_FILES
                )
                val uri = mediaFiles?.get(0)?.uri
                viewModel.pu = uri
                viewModel.updatePhoto(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.apply {

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.loggedInState.collect { user ->
                    Log.d(TAG, "user = $user")
                    if (user != null) {
                        tvLoginState.text = "You are logged in"
                        cvUsername.visibility =
                            View.VISIBLE // try to delete it and check if still works
                        etUsername.setText(viewModel.un)
                        viewModel.pu?.let {
                            Picasso.get().load(it).into(ivPhoto)
                        }
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
                        ivEdit.setOnClickListener {
                            Log.d(TAG, "onViewCreated: iv edit clicked LOGS")
                            cameraPermissionDialogLauncher.launch(Manifest.permission.CAMERA)
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
                        val action =
                            ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
                        findNavController().navigate(action)
                    }
                    is ProfileViewModel.ProfileEvent.ProfileSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is ProfileViewModel.ProfileEvent.CameraPermissionActivity -> {
//                        cameraPermissionLauncher.launch(event.perms)
                    }
                    is ProfileViewModel.ProfileEvent.PickImageActivity -> {
                        imagePickerActivityLauncher.launch(event.intent)
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





