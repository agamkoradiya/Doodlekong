package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.data.remote.ws.Room
import com.plcoding.doodlekong.databinding.FragmentCreateRoomBinding
import com.plcoding.doodlekong.ui.setup.CreateRoomViewModel
import com.plcoding.doodlekong.util.Constants
import com.plcoding.doodlekong.util.hideKeyboard
import com.plcoding.doodlekong.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class CreateRoomFragment : Fragment(R.layout.fragment_create_room) {

    private var _binding: FragmentCreateRoomBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<CreateRoomViewModel>()
    private val args by navArgs<CreateRoomFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupRoomSizeSpinner()
        listenToEvents()

        binding.btnCreateRoom.setOnClickListener {
            binding.createRoomProgressBar.isVisible = true
            viewModel.createRoom(
                Room(
                    binding.etRoomName.text.toString(),
                    binding.tvMaxPersons.text.toString().toInt()
                )
            )

            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.setupEvent.collect { event ->
                when (event) {
                    is CreateRoomViewModel.SetupEvent.CreateRoomEvent -> {
                        viewModel.joinRoom(args.username, event.room.name)
                    }
                    is CreateRoomViewModel.SetupEvent.InputEmptyError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(R.string.error_field_empty)
                    }
                    is CreateRoomViewModel.SetupEvent.InputTooShortError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(
                            getString(
                                R.string.error_room_name_too_short,
                                Constants.MIN_ROOM_NAME_LENGTH
                            )
                        )
                    }
                    is CreateRoomViewModel.SetupEvent.InputTooLongError -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(
                            getString(
                                R.string.error_room_name_too_long,
                                Constants.MIN_ROOM_NAME_LENGTH
                            )
                        )
                    }
                    is CreateRoomViewModel.SetupEvent.CreateRoomErrorEvent -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(event.error)
                    }
                    is CreateRoomViewModel.SetupEvent.JoinRoomEvent -> {
                        binding.createRoomProgressBar.isVisible = false
                        val action =
                            CreateRoomFragmentDirections.actionCreateRoomFragmentToDrawingActivity(
                                args.username,
                                event.roomName
                            )
                        findNavController().navigate(action)
                    }
                    is CreateRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                        binding.createRoomProgressBar.isVisible = false
                        snackbar(event.error)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun setupRoomSizeSpinner() {
        val roomSizes = resources.getStringArray(R.array.room_size_array)
        val adapter = ArrayAdapter(requireContext(), R.layout.textview_room_size, roomSizes)
        binding.tvMaxPersons.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}