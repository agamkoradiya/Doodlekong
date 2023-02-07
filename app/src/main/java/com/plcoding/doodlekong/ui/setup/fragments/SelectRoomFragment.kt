package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.adapters.RoomAdapter
import com.plcoding.doodlekong.databinding.FragmentSelectRoomBinding
import com.plcoding.doodlekong.ui.setup.SelectRoomViewModel
import com.plcoding.doodlekong.util.Constants.SEARCH_DELAY
import com.plcoding.doodlekong.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SelectRoomFragment : Fragment(R.layout.fragment_select_room) {

    private var _binding: FragmentSelectRoomBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<SelectRoomViewModel>()
    private val args: SelectRoomFragmentArgs by navArgs()

    private var updateRoomsJob: Job? = null

    @Inject
    lateinit var roomAdapter: RoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListener()
        subscribeToObservers()
        listenToEvents()

        viewModel.getRooms("")

        var searchJob: Job? = null
        binding.etRoomName.addTextChangedListener {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(SEARCH_DELAY)
                viewModel.getRooms(it.toString())
            }
        }

    }

    private fun setupClickListener() {
        binding.ibReload.setOnClickListener {
            binding.roomsProgressBar.isVisible = true
            binding.ivNoRoomsFound.isVisible = false
            binding.tvNoRoomsFound.isVisible = false
            viewModel.getRooms(binding.etRoomName.text.toString())
        }

        binding.btnCreateRoom.setOnClickListener {
            val action =
                SelectRoomFragmentDirections.actionSelectRoomFragmentToCreateRoomFragment(args.username)
            findNavController().navigate(action)
        }

        roomAdapter.setOnRoomClickListener {
            viewModel.joinRoom(args.username, it.name)
        }
    }

    private fun listenToEvents() = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        viewModel.setupEvent.collect { event ->
            when (event) {
                is SelectRoomViewModel.SetupEvent.JoinRoomEvent -> {
                    Timber.i("listenToEvents: Join room event called")
                    val action =
                        SelectRoomFragmentDirections.actionSelectRoomFragmentToDrawingActivity(
                            args.username,
                            event.roomName
                        )
                    findNavController().navigate(action)
                }
                is SelectRoomViewModel.SetupEvent.JoinRoomErrorEvent -> {
                    snackbar(event.error)
                }
                is SelectRoomViewModel.SetupEvent.GetRoomErrorEvent -> {
                    binding.apply {
                        roomsProgressBar.isVisible = false
                        tvNoRoomsFound.isVisible = false
                        ivNoRoomsFound.isVisible = false
                    }
                    snackbar(event.error)
                }
                else -> Unit
            }
        }
    }

    private fun subscribeToObservers() = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        viewModel.rooms.collect { event ->
            when (event) {
                is SelectRoomViewModel.SetupEvent.GetRoomLoadingEvent -> {
                    binding.roomsProgressBar.isVisible = true
                }
                is SelectRoomViewModel.SetupEvent.GetRoomEvent -> {
                    binding.roomsProgressBar.isVisible = false
                    val isRoomsEmpty = event.rooms.isEmpty()
                    binding.tvNoRoomsFound.isVisible = isRoomsEmpty
                    binding.ivNoRoomsFound.isVisible = isRoomsEmpty

                    updateRoomsJob?.cancel()
                    updateRoomsJob = lifecycleScope.launch {
                        roomAdapter.updateDataset(event.rooms)
                    }
                }
                else -> Unit
            }
        }
    }


    private fun setupRecyclerView() {
        binding.rvRooms.apply {
            adapter = roomAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}