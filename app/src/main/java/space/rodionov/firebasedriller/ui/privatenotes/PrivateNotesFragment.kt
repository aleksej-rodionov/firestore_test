package space.rodionov.firebasedriller.ui.privatenotes

import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.flow.collect
import space.rodionov.firebasedriller.R
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.databinding.FragmentPrivateNotesBinding
import timber.log.Timber

private const val TAG = "Fragment LOGS"

@AndroidEntryPoint
class PrivateNotesFragment : Fragment(R.layout.fragment_private_notes) {

    private val viewModel: PrivateNotesViewModel by viewModels()
    private var _binding: FragmentPrivateNotesBinding? = null
    private val binding get() = _binding!!

    lateinit var notesAdapter: NotesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPrivateNotesBinding.bind(view)

        viewModel.subscribeToNotes()

        notesAdapter = NotesAdapter(
            onNoteClick = {
                viewModel.editNote(it)
            },
            onCompletedCheck = { note, completed ->
                viewModel.completedCheck(note, completed)
            }
        )

        binding.apply {

            rvPrivateNotes.apply {
                adapter = notesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
//                itemAnimator = null
            }

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val note = notesAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.deleteNote(note)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    RecyclerViewSwipeDecorator.Builder(
                        c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                    ).addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                        .addActionIcon(R.drawable.ic_delete)
                        .create()
                        .decorate()

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }).attachToRecyclerView(rvPrivateNotes)

            btnAdd.setOnClickListener {
                viewModel.newNote()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.notesFlow.collect {
                Log.d(TAG, "collect it = ${it ?: null}")
                val notes = it ?: return@collect
                Log.d(TAG, "collect: CALLED notes.size = ${notes.size}")
                submitList(notes)
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.privateNotesEvent.collect { event ->
                when (event) {
                    is PrivateNotesViewModel.PrivateNotesEvent.PrivateNotesSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is PrivateNotesViewModel.PrivateNotesEvent.NavAddNote -> {
                        val action =
                            PrivateNotesFragmentDirections.actionPrivateNotesFragmentToEditNoteFragment(
                                null,
                                getString(R.string.new_note)
                            )
                        findNavController().navigate(action)
                    }
                    is PrivateNotesViewModel.PrivateNotesEvent.NavEditNote -> {
                        val action =
                            PrivateNotesFragmentDirections.actionPrivateNotesFragmentToEditNoteFragment(
                                event.note,
                                getString(R.string.edit_note)
                            )
                        findNavController().navigate(action)
                    }
                    is PrivateNotesViewModel.PrivateNotesEvent.PrivateNoteInteractiveSnackbar -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG)
                            .setAction("UNDO") { viewModel.saveNote(event.note) }
                            .show()
                    }
                }
            }
        }
    }

    fun submitList(notes: List<Note>) {
        Log.d(TAG, "submitList: CALLED")
        notesAdapter.submitList(notes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





