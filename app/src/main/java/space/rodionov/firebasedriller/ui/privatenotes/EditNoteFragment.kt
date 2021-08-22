package space.rodionov.firebasedriller.ui.privatenotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.databinding.FragmentEditNoteBinding

@AndroidEntryPoint
class EditNoteFragment : BottomSheetDialogFragment() {

    private val viewModel: EditNoteViewModel by viewModels()
    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditNoteBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            tvTitle.text = viewModel.title
            etNoteText.setText(viewModel.noteText)
            cbHighPrioroty.isChecked = viewModel.notePriority

            //==========================LISTENERS==================================
            etNoteText.addTextChangedListener {
                viewModel.noteText = it.toString()
            }
            cbHighPrioroty.setOnCheckedChangeListener { _, isChecked ->
                viewModel.notePriority = isChecked
            }

            btnSave.setOnClickListener {
                viewModel.onSaveClick()
//                viewModel.showUserId()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.editNoteEvent.collect { event ->
                when (event) {
                    is EditNoteViewModel.EditNoteEvent.EditNoteSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is EditNoteViewModel.EditNoteEvent.NavBackWithResult -> {
                        binding.etNoteText.clearFocus()
                        setFragmentResult("add_edit_request", bundleOf("add_edit_result" to event.result))
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        //this forces the sheet to appear at max height even on landscape
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





