package space.rodionov.firebasedriller.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.databinding.FragmentUploadLocalNotesBinding

private const val TAG = "Fragment LOGS"

@AndroidEntryPoint
class UploadLocalNotesFragment : Fragment(R.layout.fragment_upload_local_notes) {

    private var _binding: FragmentUploadLocalNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UploadLocalNotesViewModel by viewModels()

    lateinit var noteAdapter: LocalNotesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUploadLocalNotesBinding.bind(view)

        noteAdapter = LocalNotesAdapter(
            viewLifecycleOwner.lifecycleScope,
            viewModel.bufferFlow,
            onNoteCheck = { note, isChecked ->
                viewModel.onNoteCheck(note, isChecked)
            }
        )

        binding.apply {
            rvLocalNotes.apply {
                adapter = noteAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            viewModel.areAllChecked.observe(viewLifecycleOwner) {
                cbChooseAll.isChecked = it
            }

            cbChooseAll.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onCheckAll(isChecked)
                noteAdapter.notifyDataSetChanged()
            }

            btnAction.text = getString(R.string.upload)

            btnAction.setOnClickListener {
                viewModel.upload()
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.allLocalNotes.collect {
                    val notes = it ?: return@collect
                    noteAdapter.submitList(notes)
                    viewModel.notes = it.toMutableList()

                    if (notes.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        cbChooseAll.isChecked = false
                        btnAction.text = getString(R.string.ok)
                        btnAction.setOnClickListener {
                            viewModel.goToPrivateNotes()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uploadLocalNotesEvent.collect { event ->
                when (event) {
                    is UploadLocalNotesViewModel.UploadLocalNotesEvent.UploadLocalNotesSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is UploadLocalNotesViewModel.UploadLocalNotesEvent.NavigateUploadLocalToPrivateNotes -> {
                        val action = UploadLocalNotesFragmentDirections.actionUploadLocalNotesFragmentToPrivateNotesFragment()
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





