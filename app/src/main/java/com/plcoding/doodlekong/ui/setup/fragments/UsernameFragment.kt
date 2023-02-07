package com.plcoding.doodlekong.ui.setup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.plcoding.doodlekong.R
import com.plcoding.doodlekong.databinding.FragmentUsernameBinding
import com.plcoding.doodlekong.ui.setup.UsernameViewModel
import com.plcoding.doodlekong.util.Constants.MAX_USERNAME_LENGTH
import com.plcoding.doodlekong.util.Constants.MIN_USERNAME_LENGTH
import com.plcoding.doodlekong.util.hideKeyboard
import com.plcoding.doodlekong.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UsernameFragment : Fragment(R.layout.fragment_username) {

    private var _binding: FragmentUsernameBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<UsernameViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listenToEvents()
        binding.btnNext.setOnClickListener {
            viewModel.validateUsernameAndNavigateToSelectRoom(
                binding.etUsername.text.toString()
            )
            requireActivity().hideKeyboard(binding.root)
        }
    }

    private fun listenToEvents() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.setupEvent.collect { event ->
                when (event) {
                    is UsernameViewModel.SetupEvent.InputEmptyError -> {
                        snackbar(R.string.error_field_empty)
                    }
                    is UsernameViewModel.SetupEvent.InputTooLongError -> {
                        snackbar(getString(R.string.error_username_too_long, MAX_USERNAME_LENGTH))
                    }
                    is UsernameViewModel.SetupEvent.InputTooShortError -> {
                        snackbar(getString(R.string.error_username_too_short, MIN_USERNAME_LENGTH))
                    }
                    is UsernameViewModel.SetupEvent.NavigateToSelectRoomEvent -> {
                        val action =
                            UsernameFragmentDirections.actionUsernameFragmentToSelectRoomFragment(
                                event.username
                            )
                        findNavController().navigate(action)
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}